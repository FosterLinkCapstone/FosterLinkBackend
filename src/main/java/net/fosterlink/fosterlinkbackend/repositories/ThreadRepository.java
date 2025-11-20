package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ThreadRepository extends CrudRepository<ThreadEntity, Integer> {

    List<ThreadEntity> findByTitleContaining(String title);
    List<ThreadEntity> findByCreatedAtBetween(Date start, Date end);
    @Query(value = "SELECT * FROM thread;", nativeQuery = true)
    List<ThreadEntity> getAll();

}
