package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.FAQRequestEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

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

}
