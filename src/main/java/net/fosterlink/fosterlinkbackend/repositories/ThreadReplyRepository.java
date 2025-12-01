package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadReplyEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ThreadReplyRepository extends CrudRepository<ThreadReplyEntity, Integer> {

    @Query(value = """
    SELECT
    t.id,
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
    u.created_at as author_created_at
FROM thread_reply t
INNER JOIN user u ON t.posted_by = u.id
LEFT JOIN (
    SELECT thread, COUNT(*) as like_count
    FROM thread_reply_like
    GROUP BY thread
) likes ON t.id = likes.thread
LEFT JOIN thread_reply_like user_like
    ON t.id = user_like.thread AND user_like.user = :userId
WHERE t.thread_id = :threadId
GROUP BY t.id, t.content, t.created_at, t.updated_at,
         likes.like_count, user_like.thread, u.id, u.first_name, u.last_name,
         u.username, u.profile_picture_url, u.verified_foster,
         u.faq_author, u.verified_agency_rep, u.created_at
    """, nativeQuery = true)
    List<Object[]> getRepliesForThread(int threadId, int userId); // -1 if no user

}
