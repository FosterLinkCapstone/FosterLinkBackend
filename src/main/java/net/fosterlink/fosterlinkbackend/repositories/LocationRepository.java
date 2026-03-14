package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface LocationRepository extends CrudRepository<LocationEntity, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM location WHERE id NOT IN (SELECT address FROM agency WHERE address IS NOT NULL)", nativeQuery = true)
    int deleteOrphanedLocations();
}
