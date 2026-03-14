package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.RefreshTokenEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.repositories.RefreshTokenRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    @Value("${app.refreshTokenExpirationMs}")
    private long refreshTokenExpirationMs;

    @Value("${app.refreshTokenExpirationLongMs}")
    private long refreshTokenExpirationLongMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Creates a new refresh token for the given user and persists its hash.
     *
     * @param user      the user to issue a token for
     * @param longLived if true, uses the long-lived expiry (stay logged in); otherwise uses the session expiry
     * @return the plain token value (UUID string) -- only time this is available unhashed; caller must set it in the response cookie
     */
    public String createRefreshToken(UserEntity user, boolean longLived) {
        String plainToken = UUID.randomUUID().toString();
        String hash = sha256(plainToken);

        long expiryMs = longLived ? refreshTokenExpirationLongMs : refreshTokenExpirationMs;

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(user.getId());
        entity.setTokenHash(hash);
        entity.setExpiresAt(Instant.now().plusMillis(expiryMs));
        entity.setRevoked(false);
        entity.setCreatedAt(Instant.now());

        refreshTokenRepository.save(entity);
        return plainToken;
    }

    /**
     * Validates an incoming plain refresh token, revokes it, and issues a replacement.
     * Implements mandatory rotation: old token is always revoked on success.
     *
     * @param rawToken the plain token value read from the cookie
     * @return a {@link RotateResult} with the new plain token and the associated user, or empty if invalid/expired
     */
    @Transactional
    public Optional<RotateResult> validateAndRotate(String rawToken) {
        String hash = sha256(rawToken);
        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        RefreshTokenEntity existing = tokenOpt.get();

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            // Expired -- revoke it and return empty
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            return Optional.empty();
        }

        Optional<UserEntity> userOpt = userRepository.findById(existing.getUserId());
        if (userOpt.isEmpty()) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            return Optional.empty();
        }

        // Determine if the original token was long-lived based on remaining duration
        boolean wasLongLived = existing.getExpiresAt()
                .isAfter(Instant.now().plusMillis(refreshTokenExpirationMs));

        // Revoke old token
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        // Issue new token with same expiry class
        String newPlainToken = createRefreshToken(userOpt.get(), wasLongLived);

        return Optional.of(new RotateResult(newPlainToken, userOpt.get(), wasLongLived));
    }

    /**
     * Revokes a single refresh token by its hash (derived here from the plain token).
     * Used during single-device logout.
     */
    @Transactional
    public void revokeByRawToken(String rawToken) {
        String hash = sha256(rawToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    /**
     * Revokes all refresh tokens for a user.
     * Used during logout-all / account deletion.
     */
    @Transactional
    public void revokeAllForUser(int userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    // Runs daily at 01:00. Removes expired and revoked refresh tokens so the table
    // does not grow unboundedly. This satisfies the RETENTION-04 compliance requirement.
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedTrue(Instant.now());
        log.info("Purged expired and revoked refresh tokens");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record RotateResult(String newPlainToken, UserEntity user, boolean longLived) {}
}
