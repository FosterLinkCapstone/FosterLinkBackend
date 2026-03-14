package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Integer> {

    Optional<RefreshTokenEntity> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM refresh_token WHERE user_id = :userId", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") int userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM refresh_token WHERE token_hash = :tokenHash", nativeQuery = true)
    void deleteByTokenHash(@Param("tokenHash") String tokenHash);

    /** Bulk-deletes all tokens that have passed their expiry time or have been explicitly revoked. */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM refresh_token WHERE expires_at < :cutoff OR revoked = 1", nativeQuery = true)
    void deleteByExpiresAtBeforeOrRevokedTrue(@Param("cutoff") Instant cutoff);

}
