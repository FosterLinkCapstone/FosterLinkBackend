package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.AccountDeletionRequestEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AccountDeletionRequestRepository extends CrudRepository<AccountDeletionRequestEntity, Integer> {

    @Query(value = """
        SELECT
            dr.id,
            dr.requested_at,
            dr.auto_approve_by,
            dr.reviewed_at,
            dr.auto_approved,
            dr.approved,
            dr.delay_note,
            dr.clear_account,
            u.id AS user_id,
            u.first_name,
            u.last_name,
            u.username,
            u.profile_picture_url,
            u.verified_foster,
            u.created_at AS user_created_at,
            IFNULL(rv.id, 0) AS reviewer_id,
            IFNULL(rv.first_name, '') AS reviewer_first_name,
            IFNULL(rv.last_name, '') AS reviewer_last_name,
            IFNULL(rv.username, '') AS reviewer_username,
            IFNULL(rv.profile_picture_url, '') AS reviewer_profile_picture_url,
            IFNULL(rv.verified_foster, 0) AS reviewer_verified_foster,
            IFNULL(rv.created_at, NOW()) AS reviewer_created_at
        FROM account_deletion_request dr
        INNER JOIN `user` u ON dr.requested_by = u.id
        LEFT JOIN `user` rv ON dr.reviewed_by = rv.id
        WHERE dr.approved = 0 AND dr.reviewed_at IS NULL
        ORDER BY dr.requested_at DESC
    """, nativeQuery = true)
    List<Object[]> findAllPendingByRecency(Pageable pageable);

    @Query(value = """
        SELECT
            dr.id,
            dr.requested_at,
            dr.auto_approve_by,
            dr.reviewed_at,
            dr.auto_approved,
            dr.approved,
            dr.delay_note,
            dr.clear_account,
            u.id AS user_id,
            u.first_name,
            u.last_name,
            u.username,
            u.profile_picture_url,
            u.verified_foster,
            u.created_at AS user_created_at,
            IFNULL(rv.id, 0) AS reviewer_id,
            IFNULL(rv.first_name, '') AS reviewer_first_name,
            IFNULL(rv.last_name, '') AS reviewer_last_name,
            IFNULL(rv.username, '') AS reviewer_username,
            IFNULL(rv.profile_picture_url, '') AS reviewer_profile_picture_url,
            IFNULL(rv.verified_foster, 0) AS reviewer_verified_foster,
            IFNULL(rv.created_at, NOW()) AS reviewer_created_at
        FROM account_deletion_request dr
        INNER JOIN `user` u ON dr.requested_by = u.id
        LEFT JOIN `user` rv ON dr.reviewed_by = rv.id
        WHERE dr.approved = 0 AND dr.reviewed_at IS NULL
        ORDER BY dr.auto_approve_by ASC
    """, nativeQuery = true)
    List<Object[]> findAllPendingByUrgency(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM account_deletion_request WHERE approved = 0 AND reviewed_at IS NULL", nativeQuery = true)
    int countPending();

    @Query("SELECT dr FROM AccountDeletionRequestEntity dr JOIN FETCH dr.requestedBy WHERE dr.requestedBy.id = :userId AND dr.approved = false AND dr.reviewedAt IS NULL")
    Optional<AccountDeletionRequestEntity> findPendingByUserId(@Param("userId") int userId);

    @Query(value = "SELECT dr.* FROM account_deletion_request dr WHERE dr.approved = 0 AND dr.reviewed_at IS NULL AND dr.auto_approve_by <= NOW()", nativeQuery = true)
    List<AccountDeletionRequestEntity> findAllPastAutoApproveDate();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM account_deletion_request WHERE requested_by = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") int userId);

    /**
     * Finds pending requests whose auto-approval date falls within the next 7 days
     * and is still at least 1 day away (to avoid re-sending the warning repeatedly).
     */
    @Query("SELECT dr FROM AccountDeletionRequestEntity dr JOIN FETCH dr.requestedBy " +
           "WHERE dr.approved = false AND dr.reviewedAt IS NULL " +
           "AND dr.autoApproveBy > :now AND dr.autoApproveBy <= :sevenDaysFromNow")
    List<AccountDeletionRequestEntity> findApproachingAutoApproval(
            @Param("now") java.util.Date now,
            @Param("sevenDaysFromNow") java.util.Date sevenDaysFromNow);
}
