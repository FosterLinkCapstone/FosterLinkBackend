package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import org.springframework.data.repository.CrudRepository;

public interface AgencyRepository extends CrudRepository<AgencyEntity, Integer> {
}
