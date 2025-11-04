package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadReplyLikeEntity;
import org.springframework.data.repository.CrudRepository;

public interface ThreadReplyLikeRepository extends CrudRepository<ThreadReplyLikeEntity, Integer> {
}
