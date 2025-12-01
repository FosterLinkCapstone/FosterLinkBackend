package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.FAQApprovalEntity;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import org.springframework.data.repository.CrudRepository;

public interface FAQApprovalRepository extends CrudRepository<FAQApprovalEntity, Integer> {
}
