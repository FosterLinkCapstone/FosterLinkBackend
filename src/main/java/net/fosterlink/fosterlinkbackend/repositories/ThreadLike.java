package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadLikeEntity;
import org.springframework.data.repository.CrudRepository;

public interface ThreadLike extends CrudRepository<ThreadLikeEntity, Integer> {
}
