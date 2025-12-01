package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadTagEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ThreadTagRepository extends CrudRepository<ThreadTagEntity, Integer> {

    List<ThreadTagEntity> searchByName(String name);

}
