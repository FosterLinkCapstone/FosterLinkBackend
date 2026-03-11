package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.DontSendEmailEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DontSendEmailRepository extends CrudRepository<DontSendEmailEntity, Integer> {

    boolean existsByUserIdAndEmailTypeId(int userId, int emailTypeId);

    List<DontSendEmailEntity> findAllByUserId(int userId);

    void deleteByUserIdAndEmailTypeId(int userId, int emailTypeId);

    void deleteAllByUserId(int userId);
}
