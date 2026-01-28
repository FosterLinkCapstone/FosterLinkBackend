package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadLikeEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ThreadLikeRepository extends CrudRepository<ThreadLikeEntity, Integer> {

    @Query("SELECT COUNT(tl) FROM ThreadLikeEntity tl WHERE tl.thread = :threadId")
    int likeCountForThread(int threadId);
    
    @Query("SELECT tl.thread, COUNT(tl) FROM ThreadLikeEntity tl WHERE tl.thread IN :threadIds GROUP BY tl.thread")
    List<Object[]> likeCountsForThreads(@Param("threadIds") List<Integer> threadIds);
    
    @Transactional
    void deleteThreadLikeEntityByThreadAndUser(int id, UserEntity user);

    boolean existsByThreadAndUser(int thread, UserEntity user);
}
