package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.FAQRequestEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FAQRequestRepository extends CrudRepository<FAQRequestEntity, Integer> {

    @Query(value = """
        SELECT fa.id, fa.suggested_topic, rq.username
        FROM faq_request fa
        INNER JOIN user rq ON fa.requested_by = rq.id
        GROUP BY fa.id
        ORDER BY rand()
        LIMIT 100;
    """, nativeQuery = true)
    List<Object[]> getAllRequests();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM faq_request WHERE requested_by = :userId", nativeQuery = true)
    void deleteByRequestedById(@Param("userId") int userId);

}
