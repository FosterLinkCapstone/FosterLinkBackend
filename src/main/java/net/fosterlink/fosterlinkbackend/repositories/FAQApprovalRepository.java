package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.FAQApprovalEntity;
import net.fosterlink.fosterlinkbackend.models.rest.ApprovalCheckResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FAQApprovalRepository extends CrudRepository<FAQApprovalEntity, Integer> {

    Optional<FAQApprovalEntity> findFAQApprovalEntityByFaqId(Integer faq_id);

    @org.springframework.transaction.annotation.Transactional
    void deleteByFaqId(int faqId);
    @Query(value = """
    SELECT
        SUM(CASE WHEN appr.approved IS NULL THEN 1 ELSE 0 END) as countPending,
        SUM(CASE WHEN appr.approved = FALSE THEN 1 ELSE 0 END) as countDenied
    FROM faq fa
    LEFT JOIN faq_approval appr ON appr.faq_id = fa.id
    WHERE fa.author = :userId
    LIMIT 1
    """, nativeQuery = true)
    List<Object[]> getApprovalCountsForUser(@Param("userId") int userId);

}
