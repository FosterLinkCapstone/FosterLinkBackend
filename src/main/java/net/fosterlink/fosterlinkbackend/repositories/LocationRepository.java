package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LocationRepository extends CrudRepository<LocationEntity, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM location WHERE id NOT IN (SELECT address FROM agency WHERE address IS NOT NULL)", nativeQuery = true)
    int deleteOrphanedLocations();

    /** Batch-deletes location rows by ID. Use after deleting dependent agencies. */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM location WHERE id IN :ids", nativeQuery = true)
    void deleteAllByIds(@Param("ids") List<Integer> ids);
}
