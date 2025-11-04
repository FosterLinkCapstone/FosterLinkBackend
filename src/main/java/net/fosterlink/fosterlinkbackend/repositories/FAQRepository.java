package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import org.springframework.data.repository.CrudRepository;

public interface FAQRepository extends CrudRepository<FaqEntity, Integer> {
}
