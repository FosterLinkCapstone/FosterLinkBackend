package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ThreadRepository extends CrudRepository<ThreadEntity, Integer> {

    List<ThreadEntity> findByTitleContaining(String title);
    List<ThreadEntity> findByContentContaining(String content);
    List<ThreadEntity> findByCreatedAtBetween(Date start, Date end);
    @Query(value = "SELECT * FROM thread;", nativeQuery = true)
    List<ThreadEntity> getAll();
    @Query(value = """
    SELECT
        t.id,
        t.title,
        t.content,
        t.created_at,
        t.updated_at,
        COALESCE(likes.like_count, 0) as like_count,
        IF(user_like.thread IS NOT NULL, true, false) AS is_liked,
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
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE t.posted_by = :userId
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, u.id, u.first_name, u.last_name,
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
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, u.id, u.first_name, u.last_name,
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
    LEFT JOIN (
        SELECT thread, COUNT(*) as like_count
        FROM thread_like
        GROUP BY thread
    ) likes ON t.id = likes.thread
    LEFT JOIN thread_like user_like
        ON t.id = user_like.thread AND user_like.user = :userId
    LEFT JOIN thread_tag tt ON t.id = tt.thread
    WHERE t.id = :threadId
    GROUP BY t.id, t.title, t.content, t.created_at, t.updated_at,
             likes.like_count, u.id, u.first_name, u.last_name,
             u.username, u.profile_picture_url, u.verified_foster,
             u.faq_author, u.verified_agency_rep, u.created_at
    LIMIT 1
""", nativeQuery = true)
    List<Object[]> findByIdResponse(@Param("threadId") int threadId, int userId); // -1 if no user
}
