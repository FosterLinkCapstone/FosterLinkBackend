package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadLikeEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ThreadLikeRepository extends CrudRepository<ThreadLikeEntity, Integer> {

    @Query("SELECT COUNT(tl) FROM ThreadLikeEntity tl WHERE tl.thread = :threadId")
    int likeCountForThread(int threadId);
    @Transactional
    void deleteThreadLikeEntityByThreadAndUser(int id, UserEntity user);

    boolean existsByThreadAndUser(int thread, UserEntity user);
}
