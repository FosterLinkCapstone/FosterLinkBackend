package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadTagEntity;
import org.springframework.data.repository.CrudRepository;

public interface ThreadTagRepository extends CrudRepository<ThreadTagEntity, Integer> {
}
