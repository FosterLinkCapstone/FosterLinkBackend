package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.AgentEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import org.springframework.data.repository.CrudRepository;

public interface AgentRepository extends CrudRepository<AgentEntity, Integer> {
}
