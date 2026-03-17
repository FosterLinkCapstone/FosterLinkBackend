package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.MailingListEntity;
import org.springframework.data.repository.CrudRepository;

public interface MailingListRepository extends CrudRepository<MailingListEntity, Integer> {

    boolean existsByName(String name);

}
