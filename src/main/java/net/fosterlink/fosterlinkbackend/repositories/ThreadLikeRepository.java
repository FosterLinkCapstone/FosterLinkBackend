package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadLikeEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ThreadLikeRepository extends CrudRepository<ThreadLikeEntity, Integer> {

    @Query("SELECT COUNT(tl) FROM ThreadLikeEntity tl WHERE tl.thread.id = :threadId")
    int likeCountForThread(int threadId);

}
