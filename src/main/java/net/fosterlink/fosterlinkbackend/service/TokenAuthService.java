package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.TokenAuthEntity;
import net.fosterlink.fosterlinkbackend.repositories.TokenAuthRepository;
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
import java.util.Date;
import java.util.HexFormat;

@Service
public class TokenAuthService {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthService.class);

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
        entity.setToken(hashedToken);
        entity.setValidForEndpoint(endpoint);
        entity.setGeneratedByUserId(generatedByUserId);
        entity.setTargetUserId(targetUserId);
        entity.setExpiresAt(expiresAt);
        entity.setProcessId(processId);
        tokenAuthRepository.save(entity);

        return rawToken;
    }

    public String hashToken(String input) {
        byte[] hash = SHA256_DIGEST.get().digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    // Runs once per hour. Cleans up rows that have passed their expiry time so
    // the table doesn't grow unboundedly.
    @Scheduled(fixedRate = 3_600_000)
    public void purgeExpiredTokens() {
        int deleted = tokenAuthRepository.deleteExpiredTokens();
        if (deleted > 0) {
            log.info("Purged {} expired token_auth row(s)", deleted);
        }
    }

}
