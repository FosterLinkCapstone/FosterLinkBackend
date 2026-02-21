package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadReplyEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ThreadReplyRepository extends CrudRepository<ThreadReplyEntity, Integer> {

    @Query(value = """
        SELECT COUNT(*)
        FROM thread_reply tr
        INNER JOIN post_metadata pm ON tr.metadata = pm.id
        WHERE tr.thread_id = :threadId AND pm.hidden = false
        """, nativeQuery = true)
    int visibleReplyCountForThread(int threadId);
    
    @Query(value = """
        SELECT tr.thread_id, COUNT(*) as count
        FROM thread_reply tr
        INNER JOIN post_metadata pm ON tr.metadata = pm.id
        WHERE tr.thread_id IN :threadIds AND pm.hidden = false
        GROUP BY tr.thread_id
        """, nativeQuery = true)
    List<Object[]> visibleReplyCountsForThreads(@Param("threadIds") List<Integer> threadIds);

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
INNER JOIN post_metadata pm ON t.metadata = pm.id
LEFT JOIN (
    SELECT thread, COUNT(*) as like_count
    FROM thread_reply_like
    GROUP BY thread
) likes ON t.id = likes.thread
LEFT JOIN thread_reply_like user_like
    ON t.id = user_like.thread AND user_like.user = :userId
WHERE t.thread_id = :threadId AND pm.hidden = false
GROUP BY t.id, t.content, t.created_at, t.updated_at,
         likes.like_count, user_like.thread, u.id, u.first_name, u.last_name,
         u.username, u.profile_picture_url, u.verified_foster,
         u.faq_author, u.verified_agency_rep, u.created_at
    """, nativeQuery = true)
    List<Object[]> getRepliesForThread(int threadId, int userId); // -1 if no user
    
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
    u.created_at as author_created_at,
    pm.id as metadata_id,
    pm.hidden,
    pm.user_deleted,
    pm.locked,
    pm.verified,
    pm.hidden_by
FROM thread_reply t
INNER JOIN user u ON t.posted_by = u.id
INNER JOIN post_metadata pm ON t.metadata = pm.id
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
         u.faq_author, u.verified_agency_rep, u.created_at,
         pm.id, pm.hidden, pm.user_deleted, pm.locked, pm.verified, pm.hidden_by
    """, nativeQuery = true)
    List<Object[]> getAllRepliesForThreadAdmin(int threadId, int userId);

    @Query("SELECT tr FROM ThreadReplyEntity tr JOIN FETCH tr.postedBy JOIN FETCH tr.metadata WHERE tr.id = :replyId")
    java.util.Optional<ThreadReplyEntity> findByIdWithRelations(@Param("replyId") int replyId);

    @Query("SELECT tr FROM ThreadReplyEntity tr WHERE tr.thread_id = :threadId")
    List<ThreadReplyEntity> findByThreadId(@Param("threadId") int threadId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ThreadReplyEntity tr WHERE tr.thread_id = :threadId")
    void deleteByThreadId(@Param("threadId") int threadId);

}
