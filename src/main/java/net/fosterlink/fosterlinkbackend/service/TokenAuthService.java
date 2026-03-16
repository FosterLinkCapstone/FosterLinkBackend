package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.TokenAuthEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.repositories.TokenAuthRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class TokenAuthService {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthService.class);

    static final String UNSUBSCRIBE_ENDPOINT = "/unsubscribeAll";
    public static final String VERIFY_EMAIL_ENDPOINT = "/verifyEmail";
    public static final String RESET_PASSWORD_ENDPOINT = "/resetPassword";

    /**
     * Caches one MessageDigest per thread. MessageDigest is not thread-safe, so a
     * ThreadLocal gives each request thread a pre-initialized instance to reuse,
     * eliminating the JCA provider lookup that MessageDigest.getInstance() performs
     * on every call. digest(byte[]) resets the state internally, so no manual reset
     * is needed between uses.
     */
    private static final ThreadLocal<MessageDigest> SHA256_DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    });

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.tokenAuthExpirationPeriodMs}")
    private long tokenAuthExpirationPeriodMs;

    private @Autowired TokenAuthRepository tokenAuthRepository;
    private @Autowired UserRepository userRepository;

    /**
     * Generates a cryptographically random token, hashes it, persists the hash, and returns the
     * raw (unhashed) value for embedding in email links.
     *
     * @param processId a UUID shared by all tokens generated for the same logical request, used to
     *                  bulk-revoke sibling tokens once the request is approved or denied
     */
    public String generateToken(String endpoint, int generatedByUserId, Integer targetUserId, String processId) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);

        String hashedToken = hashToken(rawToken);
        Date expiresAt = new Date(System.currentTimeMillis() + tokenAuthExpirationPeriodMs);

        TokenAuthEntity entity = new TokenAuthEntity();
        entity.setTokenHash(hashedToken);
        entity.setValidForEndpoint(endpoint);
        entity.setGeneratedByUserId(generatedByUserId);
        entity.setTargetUserId(targetUserId);
        entity.setExpiresAt(expiresAt);
        entity.setProcessId(processId);
        tokenAuthRepository.save(entity);

        return rawToken;
    }

    /**
     * Generates a persistent (never-expiring) token for the given endpoint.
     * Use this for actions that should remain valid indefinitely, such as unsubscribe links.
     * The token has no processId and is never purged by the expiry cleanup job.
     */
    public String generatePersistentToken(String endpoint, int generatedByUserId, Integer targetUserId) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);

        String hashedToken = hashToken(rawToken);

        TokenAuthEntity entity = new TokenAuthEntity();
        entity.setTokenHash(hashedToken);
        entity.setValidForEndpoint(endpoint);
        entity.setGeneratedByUserId(generatedByUserId);
        entity.setTargetUserId(targetUserId);
        entity.setExpiresAt(null);
        entity.setProcessId(null);
        tokenAuthRepository.save(entity);

        return rawToken;
    }

    /**
     * Generates a fresh persistent unsubscribe token for the user, stores its SHA-256 hash in
     * user.unsubscribeTokenHash, and returns the raw token for embedding in the email link.
     * A new token_auth row is created on every call; prior unsubscribe links remain valid because
     * their token_auth rows are non-expiring and are not removed here.
     * Safe to call on every email send.
     */
    public String getOrCreateUnsubscribeToken(UserEntity user) {
        String rawToken = generatePersistentToken(UNSUBSCRIBE_ENDPOINT, user.getId(), user.getId());
        user.setUnsubscribeTokenHash(hashToken(rawToken));
        userRepository.save(user);
        return rawToken;
    }

    /**
     * Bulk variant of {@link #getOrCreateUnsubscribeToken}: generates fresh tokens for all users
     * in at most two batch operations (one saveAll for token rows, one saveAll for user rows).
     * Each user always receives a freshly generated raw token for their email link; the hash is
     * stored in user.unsubscribeTokenHash. Prior token_auth rows remain valid (non-expiring).
     *
     * @return map of userId → raw unsubscribe token (for embedding in email links)
     */
    public Map<Integer, String> getOrCreateUnsubscribeTokens(List<UserEntity> users) {
        Map<Integer, String> result = new HashMap<>();
        List<TokenAuthEntity> tokensToSave = new ArrayList<>();
        List<UserEntity> usersToUpdate = new ArrayList<>();

        for (UserEntity user : users) {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            String rawToken = HexFormat.of().formatHex(bytes);
            String hash = hashToken(rawToken);

            TokenAuthEntity entity = new TokenAuthEntity();
            entity.setTokenHash(hash);
            entity.setValidForEndpoint(UNSUBSCRIBE_ENDPOINT);
            entity.setGeneratedByUserId(user.getId());
            entity.setTargetUserId(user.getId());
            entity.setExpiresAt(null);
            entity.setProcessId(null);
            tokensToSave.add(entity);

            user.setUnsubscribeTokenHash(hash);
            usersToUpdate.add(user);
            result.put(user.getId(), rawToken);
        }

        if (!tokensToSave.isEmpty()) {
            tokenAuthRepository.saveAll(tokensToSave);
            userRepository.saveAll(usersToUpdate);
        }

        return result;
    }

    public String hashToken(String input) {
        byte[] hash = SHA256_DIGEST.get().digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    // Runs once per hour. Cleans up rows that have passed their expiry time so
    // the table doesn't grow unboundedly. Rows with NULL expiresAt are never purged.
    @Scheduled(fixedRate = 3_600_000)
    public void purgeExpiredTokens() {
        int deleted = tokenAuthRepository.deleteExpiredTokens();
        if (deleted > 0) {
            log.info("Purged {} expired token_auth row(s)", deleted);
        }
    }

}
