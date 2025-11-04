package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import org.springframework.data.repository.CrudRepository;

public interface ThreadRepository extends CrudRepository<ThreadEntity, Integer> {
}
