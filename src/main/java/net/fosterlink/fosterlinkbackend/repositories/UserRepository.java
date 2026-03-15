package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserRepository extends CrudRepository<UserEntity, Integer> {

    @Query(value = "SELECT user FROM UserEntity user")
    List<UserEntity> getAllUsers();

    UserEntity findByEmail(String email);

    boolean existsByUsernameOrEmail(String username, String email);

    UserEntity findByUsername(String username);

    @Query("SELECT CASE WHEN u.bannedAt IS NOT NULL THEN true ELSE false END FROM UserEntity u WHERE u.email = :email")
    Boolean isBannedByEmail(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u WHERE u.restrictedAt IS NOT NULL AND u.restrictedUntil IS NOT NULL AND u.restrictedUntil <= :now")
    List<UserEntity> findExpiredRestrictions(@Param("now") java.util.Date now);

    @Query("""
            SELECT u.id, u.administrator, u.faqAuthor, u.verifiedAgencyRep, a.id, a.name,
                   u.firstName, u.lastName, u.username, u.profilePictureUrl, u.verifiedFoster, u.createdAt,
                   u.bannedAt, u.restrictedAt
            FROM UserEntity u
            LEFT JOIN AgencyEntity a ON a.agent = u
            WHERE u.id = :userId
            """)
    List<Object[]> getProfileMetadataRow(@Param("userId") int userId);

    // -------------------------------------------------------------------------
    // Admin user search queries
    // Each returns columns:
    //   0:id, 1:first_name, 2:last_name, 3:username, 4:email, 5:phone_number,
    //   6:profile_picture_url, 7:administrator, 8:faq_author, 9:verified_agency_rep,
    //   10:verified_foster, 11:id_verified, 12:banned_at, 13:restricted_at,
    //   14:restricted_until, 15:post_count, 16:reply_count, 17:agency_count,
    //   18:faq_answer_count, 19:faq_suggestion_count
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT u.id, u.first_name, u.last_name, u.username, u.email, u.phone_number,
                   u.profile_picture_url, u.administrator, u.faq_author, u.verified_agency_rep,
                   u.verified_foster, u.id_verified, u.banned_at, u.restricted_at, u.restricted_until,
                   (SELECT COUNT(*) FROM thread t WHERE t.posted_by = u.id) AS post_count,
                   (SELECT COUNT(*) FROM thread_reply tr WHERE tr.posted_by = u.id) AS reply_count,
                   (SELECT COUNT(*) FROM agency a WHERE a.agent = u.id) AS agency_count,
                   (SELECT COUNT(*) FROM faq f WHERE f.author = u.id) AS faq_answer_count,
                   (SELECT COUNT(*) FROM faq_request fr WHERE fr.requested_by = u.id) AS faq_suggestion_count
            FROM user u
            WHERE u.account_deleted = false
              AND CONCAT(u.first_name, ' ', u.last_name) LIKE CONCAT('%', :query, '%')
            ORDER BY u.id
            LIMIT :pageSize OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> searchByFullName(@Param("query") String query, @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM user u WHERE u.account_deleted = false AND CONCAT(u.first_name, ' ', u.last_name) LIKE CONCAT('%', :query, '%')", nativeQuery = true)
    long countByFullName(@Param("query") String query);

    @Query(value = """
            SELECT u.id, u.first_name, u.last_name, u.username, u.email, u.phone_number,
                   u.profile_picture_url, u.administrator, u.faq_author, u.verified_agency_rep,
                   u.verified_foster, u.id_verified, u.banned_at, u.restricted_at, u.restricted_until,
                   (SELECT COUNT(*) FROM thread t WHERE t.posted_by = u.id) AS post_count,
                   (SELECT COUNT(*) FROM thread_reply tr WHERE tr.posted_by = u.id) AS reply_count,
                   (SELECT COUNT(*) FROM agency a WHERE a.agent = u.id) AS agency_count,
                   (SELECT COUNT(*) FROM faq f WHERE f.author = u.id) AS faq_answer_count,
                   (SELECT COUNT(*) FROM faq_request fr WHERE fr.requested_by = u.id) AS faq_suggestion_count
            FROM user u
            WHERE u.account_deleted = false
              AND u.username LIKE CONCAT('%', :query, '%')
            ORDER BY u.id
            LIMIT :pageSize OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> searchByUsername(@Param("query") String query, @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM user u WHERE u.account_deleted = false AND u.username LIKE CONCAT('%', :query, '%')", nativeQuery = true)
    long countByUsername(@Param("query") String query);

    @Query(value = """
            SELECT u.id, u.first_name, u.last_name, u.username, u.email, u.phone_number,
                   u.profile_picture_url, u.administrator, u.faq_author, u.verified_agency_rep,
                   u.verified_foster, u.id_verified, u.banned_at, u.restricted_at, u.restricted_until,
                   (SELECT COUNT(*) FROM thread t WHERE t.posted_by = u.id) AS post_count,
                   (SELECT COUNT(*) FROM thread_reply tr WHERE tr.posted_by = u.id) AS reply_count,
                   (SELECT COUNT(*) FROM agency a WHERE a.agent = u.id) AS agency_count,
                   (SELECT COUNT(*) FROM faq f WHERE f.author = u.id) AS faq_answer_count,
                   (SELECT COUNT(*) FROM faq_request fr WHERE fr.requested_by = u.id) AS faq_suggestion_count
            FROM user u
            WHERE u.account_deleted = false
              AND u.email LIKE CONCAT('%', :query, '%')
            ORDER BY u.id
            LIMIT :pageSize OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> searchByEmail(@Param("query") String query, @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM user u WHERE u.account_deleted = false AND u.email LIKE CONCAT('%', :query, '%')", nativeQuery = true)
    long countByEmail(@Param("query") String query);

    @Query(value = """
            SELECT u.id, u.first_name, u.last_name, u.username, u.email, u.phone_number,
                   u.profile_picture_url, u.administrator, u.faq_author, u.verified_agency_rep,
                   u.verified_foster, u.id_verified, u.banned_at, u.restricted_at, u.restricted_until,
                   (SELECT COUNT(*) FROM thread t WHERE t.posted_by = u.id) AS post_count,
                   (SELECT COUNT(*) FROM thread_reply tr WHERE tr.posted_by = u.id) AS reply_count,
                   (SELECT COUNT(*) FROM agency a WHERE a.agent = u.id) AS agency_count,
                   (SELECT COUNT(*) FROM faq f WHERE f.author = u.id) AS faq_answer_count,
                   (SELECT COUNT(*) FROM faq_request fr WHERE fr.requested_by = u.id) AS faq_suggestion_count
            FROM user u
            WHERE u.account_deleted = false
              AND u.phone_number LIKE CONCAT('%', :query, '%')
            ORDER BY u.id
            LIMIT :pageSize OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> searchByPhoneNumber(@Param("query") String query, @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM user u WHERE u.account_deleted = false AND u.phone_number LIKE CONCAT('%', :query, '%')", nativeQuery = true)
    long countByPhoneNumber(@Param("query") String query);

    @Query(value = """
            SELECT u.id, u.first_name, u.last_name, u.username, u.email, u.phone_number,
                   u.profile_picture_url, u.administrator, u.faq_author, u.verified_agency_rep,
                   u.verified_foster, u.id_verified, u.banned_at, u.restricted_at, u.restricted_until,
                   (SELECT COUNT(*) FROM thread t WHERE t.posted_by = u.id) AS post_count,
                   (SELECT COUNT(*) FROM thread_reply tr WHERE tr.posted_by = u.id) AS reply_count,
                   (SELECT COUNT(*) FROM agency a WHERE a.agent = u.id) AS agency_count,
                   (SELECT COUNT(*) FROM faq f WHERE f.author = u.id) AS faq_answer_count,
                   (SELECT COUNT(*) FROM faq_request fr WHERE fr.requested_by = u.id) AS faq_suggestion_count
            FROM user u
            WHERE u.account_deleted = false
              AND (
                (:role = 'ADMINISTRATOR'   AND u.administrator        = true) OR
                (:role = 'FAQ_AUTHOR'      AND u.faq_author           = true) OR
                (:role = 'AGENCY_REP'      AND u.verified_agency_rep  = true) OR
                (:role = 'VERIFIED_FOSTER' AND u.verified_foster      = true) OR
                (:role = 'ID_VERIFIED'     AND u.id_verified          = true)
              )
            ORDER BY u.id
            LIMIT :pageSize OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> searchByRole(@Param("role") String role, @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM user u
            WHERE u.account_deleted = false
              AND (
                (:role = 'ADMINISTRATOR'   AND u.administrator        = true) OR
                (:role = 'FAQ_AUTHOR'      AND u.faq_author           = true) OR
                (:role = 'AGENCY_REP'      AND u.verified_agency_rep  = true) OR
                (:role = 'VERIFIED_FOSTER' AND u.verified_foster      = true) OR
                (:role = 'ID_VERIFIED'     AND u.id_verified          = true)
              )
            """, nativeQuery = true)
    long countByRole(@Param("role") String role);

    @Query(value = """
            SELECT u.id, u.first_name, u.last_name, u.username, u.email, u.phone_number,
                   u.profile_picture_url, u.administrator, u.faq_author, u.verified_agency_rep,
                   u.verified_foster, u.id_verified, u.banned_at, u.restricted_at, u.restricted_until,
                   (SELECT COUNT(*) FROM thread t WHERE t.posted_by = u.id) AS post_count,
                   (SELECT COUNT(*) FROM thread_reply tr WHERE tr.posted_by = u.id) AS reply_count,
                   (SELECT COUNT(*) FROM agency a WHERE a.agent = u.id) AS agency_count,
                   (SELECT COUNT(*) FROM faq f WHERE f.author = u.id) AS faq_answer_count,
                   (SELECT COUNT(*) FROM faq_request fr WHERE fr.requested_by = u.id) AS faq_suggestion_count
            FROM user u
            WHERE u.account_deleted = false
            ORDER BY u.id
            LIMIT :pageSize OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findAllPaginated(@Param("pageSize") int pageSize, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM user u WHERE u.account_deleted = false", nativeQuery = true)
    long countAll();

    @Query(value = """
            SELECT u.id, u.first_name, u.last_name, u.username, u.email, u.phone_number,
                   u.profile_picture_url, u.administrator, u.faq_author, u.verified_agency_rep,
                   u.verified_foster, u.id_verified, u.banned_at, u.restricted_at, u.restricted_until,
                   (SELECT COUNT(*) FROM thread t WHERE t.posted_by = u.id) AS post_count,
                   (SELECT COUNT(*) FROM thread_reply tr WHERE tr.posted_by = u.id) AS reply_count,
                   (SELECT COUNT(*) FROM agency a WHERE a.agent = u.id) AS agency_count,
                   (SELECT COUNT(*) FROM faq f WHERE f.author = u.id) AS faq_answer_count,
                   (SELECT COUNT(*) FROM faq_request fr WHERE fr.requested_by = u.id) AS faq_suggestion_count
            FROM user u
            WHERE u.account_deleted = true
            ORDER BY u.id
            LIMIT :pageSize OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findDeletedPaginated(@Param("pageSize") int pageSize, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM user u WHERE u.account_deleted = true", nativeQuery = true)
    long countDeleted();

    @Query("SELECT u FROM UserEntity u WHERE u.administrator = true AND u.accountDeleted = false")
    List<UserEntity> findAllAdministrators();

    /** Bulk-clears expired temporary restrictions in a single UPDATE. Returns the number of rows affected. */
    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.restrictedAt = NULL, u.restrictedUntil = NULL WHERE u.restrictedUntil < :now")
    int clearExpiredRestrictions(@Param("now") java.util.Date now);

    @Query(value = """
            SELECT u.id, u.first_name, u.last_name, u.username, u.email, u.phone_number,
                   u.profile_picture_url, u.administrator, u.faq_author, u.verified_agency_rep,
                   u.verified_foster, u.id_verified, u.banned_at, u.restricted_at, u.restricted_until,
                   (SELECT COUNT(*) FROM thread t WHERE t.posted_by = u.id) AS post_count,
                   (SELECT COUNT(*) FROM thread_reply tr WHERE tr.posted_by = u.id) AS reply_count,
                   (SELECT COUNT(*) FROM agency a WHERE a.agent = u.id) AS agency_count,
                   (SELECT COUNT(*) FROM faq f WHERE f.author = u.id) AS faq_answer_count,
                   (SELECT COUNT(*) FROM faq_request fr WHERE fr.requested_by = u.id) AS faq_suggestion_count
            FROM user u
            WHERE u.id = :userId
            """, nativeQuery = true)
    List<Object[]> findAdminUserById(@Param("userId") int userId);
}
