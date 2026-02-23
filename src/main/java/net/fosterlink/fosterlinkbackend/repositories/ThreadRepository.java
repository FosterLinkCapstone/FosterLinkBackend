package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ThreadRepository extends CrudRepository<ThreadEntity, Integer> {

    @Query(value = """
        SELECT COUNT(*)
        FROM thread t
        INNER JOIN post_metadata pm ON t.metadata = pm.id
        WHERE t.posted_by = :userId AND pm.hidden = false
        """, nativeQuery = true)
    int visibleThreadCountForUser(@Param("userId") int userId);

    @Query(value = """
        SELECT t.posted_by, COUNT(*) as count
        FROM thread t
        INNER JOIN post_metadata pm ON t.metadata = pm.id
        WHERE t.posted_by IN :userIds AND pm.hidden = false
        GROUP BY t.posted_by
        """, nativeQuery = true)
    List<Object[]> visibleThreadCountsForUsers(@Param("userIds") List<Integer> userIds);

    @Query(value = "SELECT t.* FROM thread t INNER JOIN post_metadata pm ON t.metadata = pm.id WHERE pm.hidden = false AND t.title LIKE CONCAT('%', :title, '%')", nativeQuery = true)
    List<ThreadEntity> findByTitleContaining(@Param("title") String title, Pageable pageable);

    @Query(value = """
        SELECT t.* FROM thread t 
        INNER JOIN post_metadata pm ON t.metadata = pm.id 
        WHERE t.posted_by = :userId AND pm.hidden = false
        """, nativeQuery = true)
    List<ThreadEntity> findByPostedByAndPostMetadataHiddenFalse(@Param("userId") int userId);

    @Query("SELECT DISTINCT t FROM ThreadEntity t JOIN FETCH t.postedBy JOIN FETCH t.postMetadata WHERE t.postedBy.id = :userId AND t.postMetadata.hidden = false")
    List<ThreadEntity> findByPostedByAndPostMetadataHiddenFalseWithRelations(@Param("userId") int userId, Pageable pageable);

    @Query("SELECT t FROM ThreadEntity t JOIN FETCH t.postedBy JOIN FETCH t.postMetadata WHERE t.id = :threadId")
    java.util.Optional<ThreadEntity> findByIdWithRelations(@Param("threadId") int threadId);

    @Query("SELECT DISTINCT t FROM ThreadEntity t JOIN FETCH t.postedBy JOIN FETCH t.postMetadata WHERE t.id IN :threadIds AND t.postMetadata.hidden = false")
    List<ThreadEntity> findAllByIdWithRelations(@Param("threadIds") List<Integer> threadIds, Pageable pageable);
    @Query(value = "SELECT t.* FROM thread t INNER JOIN post_metadata pm ON t.metadata = pm.id WHERE pm.hidden = false AND t.content LIKE CONCAT('%', :content, '%')", nativeQuery = true)
    List<ThreadEntity> findByContentContaining(@Param("content") String content, Pageable pageable);
    @Query(value = "SELECT t.* FROM thread t INNER JOIN post_metadata pm ON t.metadata = pm.id WHERE pm.hidden = false AND t.created_at BETWEEN :start AND :end", nativeQuery = true)
    List<ThreadEntity> findByCreatedAtBetween(@Param("start") Date start, @Param("end") Date end);
    @Query(value = "SELECT t.* FROM thread t INNER JOIN post_metadata pm ON t.metadata = pm.id WHERE pm.hidden = false;", nativeQuery = true)
    List<ThreadEntity> getAll();

    @Query(value = """
        SELECT COUNT(t.id)
        FROM thread t
        INNER JOIN post_metadata pm ON t.metadata = pm.id
        WHERE pm.hidden = false
        """, nativeQuery = true)
    int countVisible();
    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE t.posted_by = :userId AND pm.hidden = false
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at
    ORDER BY (
        COALESCE(likes.like_count, 0) * 0.4 +
        (DATEDIFF(NOW(), t.created_at) / -365.0) * 0.6 +
        RAND() * 0.3
    ) DESC
    LIMIT 10
    """, nativeQuery = true)
    List<Object[]> findRandomWeightedThreadsForUser(int userId);

    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE t.posted_by = :authorId AND pm.hidden = false
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at
    ORDER BY t.created_at DESC
    """, nativeQuery = true)
    List<Object[]> findThreadsForUser(int userId, int authorId, Pageable pageable);

    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE pm.hidden = false
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at
    ORDER BY (
        COALESCE(likes.like_count, 0) * 0.4 +
        (DATEDIFF(NOW(), t.created_at) / -365.0) * 0.6 +
        RAND() * 0.3
    ) DESC
    LIMIT 10
    """, nativeQuery = true)
    List<Object[]> findRandomWeightedThreads(int userId);

    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE pm.hidden = false
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at
    ORDER BY t.created_at DESC
   """, nativeQuery = true)
    List<Object[]> findThreadsNewest(@Param("userId") int userId, Pageable pageable);

    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE pm.hidden = false
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at
    ORDER BY t.created_at ASC
    """, nativeQuery = true)
    List<Object[]> findThreadsOldest(@Param("userId") int userId, Pageable pageable);

    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE pm.hidden = false
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at
    ORDER BY COALESCE(likes.like_count, 0) DESC, t.created_at DESC;
    """, nativeQuery = true)
    List<Object[]> findThreadsMostLiked(@Param("userId") int userId, Pageable pageable);

    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        pm.id as pm_id,
        pm.hidden as pm_hidden,
        pm.user_deleted as pm_user_deleted,
        pm.locked as pm_locked,
        pm.verified as pm_verified,
        pm.hidden_by,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE pm.hidden = true AND pm.user_deleted = false
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at,
             pm.id, pm.hidden, pm.user_deleted, pm.locked, pm.verified, pm.hidden_by
    ORDER BY t.created_at DESC
    """, nativeQuery = true)
    List<Object[]> getHiddenThreadsAdminDeleted(@Param("userId") int userId, Pageable pageable);

    @Query(value = """
        SELECT COUNT(*)
        FROM thread t
        INNER JOIN post_metadata pm ON t.metadata = pm.id
        WHERE pm.hidden = true AND pm.user_deleted = false
        """, nativeQuery = true)
    int countHiddenThreadsAdminDeleted();

    @Query(value = """
        SELECT COUNT(*)
        FROM thread t
        INNER JOIN post_metadata pm ON t.metadata = pm.id
        WHERE pm.hidden = true AND pm.user_deleted = true
        """, nativeQuery = true)
    int countHiddenThreadsUserDeleted();

    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        pm.id as pm_id,
        pm.hidden as pm_hidden,
        pm.user_deleted as pm_user_deleted,
        pm.locked as pm_locked,
        pm.verified as pm_verified,
        pm.hidden_by,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE pm.hidden = true AND pm.user_deleted = true
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at,
             pm.id, pm.hidden, pm.user_deleted, pm.locked, pm.verified, pm.hidden_by
    ORDER BY t.created_at DESC
    """, nativeQuery = true)
    List<Object[]> getHiddenThreadsUserDeleted(@Param("userId") int userId, Pageable pageable);

    @Query(value = """
        SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE t.id = :threadId AND pm.hidden = false
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at
    LIMIT 1
""", nativeQuery = true)
    List<Object[]> findByIdResponse(@Param("threadId") int threadId, int userId); // -1 if no user

    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
        COALESCE(replies.comment_count, 0) AS comment_count,
        COALESCE(upc.user_post_count, 0) AS user_post_count,
        u.id as author_id,
        u.first_name,
        u.last_name,
        u.username,
        u.profile_picture_url,
        u.verified_foster,
        u.faq_author,
        u.verified_agency_rep,
        u.created_at as author_created_at,
        pm.id as pm_id,
        pm.hidden as pm_hidden,
        pm.user_deleted as pm_user_deleted,
        pm.locked as pm_locked,
        pm.verified as pm_verified,
        pm.hidden_by,
        GROUP_CONCAT(tt.name SEPARATOR ',') as tags
    FROM thread t
    INNER JOIN user u ON t.posted_by = u.id
    INNER JOIN post_metadata pm ON t.metadata = pm.id
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN (
        SELECT tr.thread_id, COUNT(*) as comment_count
        FROM thread_reply tr
        INNER JOIN post_metadata pmr ON tr.metadata = pmr.id
        WHERE pmr.hidden = false
        GROUP BY tr.thread_id
    ) replies ON t.id = replies.thread_id
    LEFT JOIN (
        SELECT t2.posted_by, COUNT(*) as user_post_count
        FROM thread t2
        INNER JOIN post_metadata pm2 ON t2.metadata = pm2.id
        WHERE pm2.hidden = false
        GROUP BY t2.posted_by
    ) upc ON u.id = upc.posted_by
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE t.id = :threadId AND pm.hidden = true
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, replies.comment_count, upc.user_post_count,
             u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at,
             pm.id, pm.hidden, pm.user_deleted, pm.locked, pm.verified, pm.hidden_by
    LIMIT 1
    """, nativeQuery = true)
    List<Object[]> findHiddenThreadById(@Param("threadId") int threadId, @Param("userId") int userId);

}
