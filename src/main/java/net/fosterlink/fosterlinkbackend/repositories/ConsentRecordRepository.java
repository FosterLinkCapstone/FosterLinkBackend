package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ConsentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecordEntity, Long> {

    List<ConsentRecordEntity> findByUserId(int userId);

}
