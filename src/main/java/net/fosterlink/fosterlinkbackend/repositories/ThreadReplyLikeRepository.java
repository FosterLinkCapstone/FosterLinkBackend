package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadReplyLikeEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface ThreadReplyLikeRepository extends CrudRepository<ThreadReplyLikeEntity, Integer> {

    @Query("SELECT COUNT(tl) FROM ThreadReplyLikeEntity tl WHERE tl.thread = :threadId")
    int likeCountForReply(int threadId);
    boolean existsByThreadAndUser(int thread, UserEntity user);
    @Transactional
    void deleteThreadReplyLikeEntitiesByThreadAndUser(int thread, UserEntity user);

    @Transactional
    void deleteByThreadIn(Collection<Integer> replyIds);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM thread_reply_like WHERE user = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") int userId);

}
