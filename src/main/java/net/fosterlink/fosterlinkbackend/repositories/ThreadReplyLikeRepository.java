package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadReplyLikeEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ThreadReplyLikeRepository extends CrudRepository<ThreadReplyLikeEntity, Integer> {

    @Query("SELECT COUNT(tl) FROM ThreadReplyLikeEntity tl WHERE tl.thread = :threadId")
    int likeCountForReply(int threadId);
    boolean existsByThreadAndUser(int thread, UserEntity user);
    @Transactional
    void deleteThreadReplyLikeEntitiesByThreadAndUser(int thread, UserEntity user);

    @Transactional
    void deleteByThreadIn(java.util.Collection<Integer> replyIds);

}
