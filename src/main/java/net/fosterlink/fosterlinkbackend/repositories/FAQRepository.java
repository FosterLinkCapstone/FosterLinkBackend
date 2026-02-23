package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FAQRepository extends CrudRepository<FaqEntity, Integer> {

    @Query(value = "SELECT COUNT(*) FROM faq fr INNER JOIN faq_approval fa ON fa.faq_id = fr.id WHERE fa.approved = true AND fa.hidden_by IS NULL", nativeQuery = true)
    int countApproved();

    @Query(value = "SELECT COUNT(*) FROM faq fr LEFT JOIN faq_approval fa ON fa.faq_id = fr.id WHERE (fa.approved IS NULL OR fa.approved = false) AND fa.hidden_by IS NULL", nativeQuery = true)
    int countPending();

    @Query(value = """
            SELECT fr.id,
                fr.title,
                fr.summary,
                fr.created_at,
                fr.updated_at,
                IFNULL(approval.approved, false) AS approved,
                IFNULL(approval.approved_by_username, '') AS approved_by_username,
                u.id as author_id,
                u.first_name,
                u.last_name,
                u.username,
                u.profile_picture_url,
                u.verified_foster,
                u.faq_author,
                u.verified_agency_rep,
                u.created_at as author_created_at
        FROM faq fr
        INNER JOIN user u ON fr.author = u.id
        LEFT JOIN (
                SELECT faq_id, approved, author.username AS approved_by_username, fa.hidden_by
                FROM faq_approval fa
                INNER JOIN user author ON fa.approved_by_id = author.id
            ) approval ON approval.faq_id = fr.id
        WHERE approved = true AND approval.hidden_by IS NULL
        GROUP BY fr.id, fr.title, fr.summary, fr.created_at, fr.updated_at, approval.approved, approval.approved_by_username, u.id,\s
                     u.first_name, u.last_name, u.profile_picture_url, u.verified_foster,\s
                     u.faq_author, u.verified_foster, u.created_at;
    """, nativeQuery = true)
    List<Object[]> allApprovedPreviews(Pageable pageable);

    @Query(value = """
            SELECT fr.id,
                fr.title,
                fr.summary,
                fr.created_at,
                fr.updated_at,
                IFNULL(approval.approved, false) AS approved,
                IFNULL(approval.approved_by_username, '') AS approved_by_username,
                u.id as author_id,
                u.first_name,
                u.last_name,
                u.username,
                u.profile_picture_url,
                u.verified_foster,
                u.faq_author,
                u.verified_agency_rep,
                u.created_at as author_created_at
        FROM faq fr
        INNER JOIN user u ON fr.author = u.id
        LEFT JOIN (
                SELECT faq_id, approved, author.username AS approved_by_username, fa.hidden_by
                FROM faq_approval fa
                INNER JOIN user author ON fa.approved_by_id = author.id
            ) approval ON approval.faq_id = fr.id
        WHERE approved = true AND fr.author = :userId AND approval.hidden_by IS NULL
        GROUP BY fr.id, fr.title, fr.summary, fr.created_at, fr.updated_at, approval.approved, approval.approved_by_username, u.id,\s
                     u.first_name, u.last_name, u.profile_picture_url, u.verified_foster,\s
                     u.faq_author, u.verified_foster, u.created_at;
    """, nativeQuery = true)
    List<Object[]> allApprovedPreviewsForUser(int userId, Pageable pageable);

    @Query(value = """
    SELECT
        fr.id,
        fr.title,
        fr.summary,
        fr.created_at,
        fr.updated_at,
        CASE
            WHEN fa.approved IS NULL THEN 3
            WHEN fa.approved IS TRUE THEN 1
            WHEN fa.approved IS FALSE THEN 2
        END as approval_status,
        fa.approved_by_username,
        u.id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at
    FROM faq fr
    INNER JOIN user u ON fr.author = u.id
    LEFT JOIN (
            SELECT faq_id, approved, ab.username AS approved_by_username, faq_approval.hidden_by FROM faq_approval
            LEFT JOIN user ab ON ab.id = approved_by_id
            ) fa ON fa.faq_id = fr.id
    WHERE (fa.approved IS NULL OR fa.approved = false) AND fa.hidden_by IS NULL
    """, nativeQuery = true)
    List<Object[]> allPendingPreviews(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM faq fr INNER JOIN faq_approval fa ON fa.faq_id = fr.id WHERE fa.hidden_by IS NOT NULL AND fa.hidden_by_author = false", nativeQuery = true)
    int countHiddenByAdmin();

    @Query(value = "SELECT COUNT(*) FROM faq fr INNER JOIN faq_approval fa ON fa.faq_id = fr.id WHERE fa.hidden_by IS NOT NULL AND fa.hidden_by_author = true", nativeQuery = true)
    int countHiddenByUser();

    @Query(value = """
    SELECT
        fr.id,
        fr.title,
        fr.summary,
        fr.created_at,
        fr.updated_at,
        fa.hidden_by,
        fa.hidden_by_author,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at
    FROM faq fr
    INNER JOIN user u ON fr.author = u.id
    INNER JOIN faq_approval fa ON fa.faq_id = fr.id
    WHERE fa.hidden_by IS NOT NULL AND fa.hidden_by_author = false
    """, nativeQuery = true)
    List<Object[]> allHiddenByAdminPreviews(Pageable pageable);

    @Query(value = """
    SELECT
        fr.id,
        fr.title,
        fr.summary,
        fr.created_at,
        fr.updated_at,
        fa.hidden_by,
        fa.hidden_by_author,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at
    FROM faq fr
    INNER JOIN user u ON fr.author = u.id
    INNER JOIN faq_approval fa ON fa.faq_id = fr.id
    WHERE fa.hidden_by IS NOT NULL AND fa.hidden_by_author = true
    """, nativeQuery = true)
    List<Object[]> allHiddenByUserPreviews(Pageable pageable);

}
