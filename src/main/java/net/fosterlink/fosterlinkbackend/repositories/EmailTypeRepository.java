package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.EmailTypeEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface EmailTypeRepository extends CrudRepository<EmailTypeEntity, Integer> {

    Optional<EmailTypeEntity> findByName(String name);

    List<EmailTypeEntity> findByCanDisableTrue();
}
