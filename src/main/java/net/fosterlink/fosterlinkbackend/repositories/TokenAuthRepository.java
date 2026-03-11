package net.fosterlink.fosterlinkbackend.repositories;


import net.fosterlink.fosterlinkbackend.entities.TokenAuthEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface TokenAuthRepository extends CrudRepository<TokenAuthEntity, Integer> {

    @Query("SELECT t FROM TokenAuthEntity t WHERE t.token = :token AND t.validForEndpoint = :endpoint AND t.expiresAt > CURRENT_TIMESTAMP")
    Optional<TokenAuthEntity> findByTokenAndEndpointNonExpired(String token, String endpoint);

    @Query("SELECT t FROM TokenAuthEntity t WHERE t.token = :token AND t.expiresAt > CURRENT_TIMESTAMP")
    Optional<TokenAuthEntity> findByTokenNonExpired(String token);

    /**
     * Atomically consumes a valid, non-expired token for the given endpoint.
     * Returns the number of rows deleted (1 = valid token consumed, 0 = not found or expired).
     * Using a single DELETE prevents a TOCTOU race where two concurrent requests both
     * pass a preceding SELECT check before either deletes the row.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenAuthEntity t WHERE t.token = :token AND t.validForEndpoint = :endpoint AND t.expiresAt > CURRENT_TIMESTAMP AND (t.targetUserId IS NULL OR t.targetUserId = :userId)")
    int consumeValidToken(String token, String endpoint, int userId);

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
    @Query("DELETE FROM TokenAuthEntity t WHERE t.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpiredTokens();

}
