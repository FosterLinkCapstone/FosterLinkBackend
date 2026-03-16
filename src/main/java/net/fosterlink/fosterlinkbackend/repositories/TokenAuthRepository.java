package net.fosterlink.fosterlinkbackend.repositories;


import net.fosterlink.fosterlinkbackend.entities.TokenAuthEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface TokenAuthRepository extends CrudRepository<TokenAuthEntity, Integer> {

    @Query("SELECT t FROM TokenAuthEntity t WHERE t.tokenHash = :tokenHash AND t.validForEndpoint = :endpoint AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP)")
    Optional<TokenAuthEntity> findByTokenAndEndpointNonExpired(String tokenHash, String endpoint);

    @Query("SELECT t FROM TokenAuthEntity t WHERE t.tokenHash = :tokenHash AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP)")
    Optional<TokenAuthEntity> findByTokenNonExpired(String tokenHash);

    /**
     * Atomically consumes a valid, non-expired token for the given endpoint.
     * Returns the number of rows deleted (1 = valid token consumed, 0 = not found or expired).
     * Using a single DELETE prevents a TOCTOU race where two concurrent requests both
     * pass a preceding SELECT check before either deletes the row.
     * NULL expiresAt is treated as non-expiring.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenAuthEntity t WHERE t.tokenHash = :tokenHash AND t.validForEndpoint = :endpoint AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP) AND (t.targetUserId IS NULL OR t.targetUserId = :userId)")
    int consumeValidToken(String tokenHash, String endpoint, int userId);

    /**
     * Read-only validation for non-consuming endpoints (e.g. unsubscribe).
     * Returns true if a matching valid token exists (not expired or indefinite).
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TokenAuthEntity t WHERE t.tokenHash = :tokenHash AND t.validForEndpoint = :endpoint AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP) AND (t.targetUserId IS NULL OR t.targetUserId = :userId)")
    boolean existsValidToken(String tokenHash, String endpoint, int userId);

    /**
     * Deletes all tokens that share the same processId.
     * Used by the deny/cancel flow and the approve flow to revoke every outstanding token for a
     * given request, so a single approve or deny invalidates all other founders' links.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenAuthEntity t WHERE t.processId = :processId")
    int deleteAllByProcessId(String processId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenAuthEntity t WHERE t.expiresAt IS NOT NULL AND t.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpiredTokens();

}
