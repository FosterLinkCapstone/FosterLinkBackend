package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.FAQApprovalEntity;
import net.fosterlink.fosterlinkbackend.models.rest.ApprovalCheckResponse;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FAQApprovalRepository extends CrudRepository<FAQApprovalEntity, Integer> {

    Optional<FAQApprovalEntity> findFAQApprovalEntityByFaqId(Integer faq_id);

    @Transactional
    void deleteByFaqId(int faqId);

    @Modifying
    @Transactional
    @Query(value = "DELETE fa FROM faq_approval fa INNER JOIN faq f ON fa.faq_id = f.id WHERE f.author = :userId", nativeQuery = true)
    void deleteByFaqAuthorId(@Param("userId") int userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE faq_approval fa INNER JOIN faq f ON fa.faq_id = f.id SET fa.hidden_by = :username, fa.hidden_by_author = true WHERE f.author = :userId AND fa.approved = true AND fa.hidden_by IS NULL", nativeQuery = true)
    void hideApprovedFaqsByAuthorId(@Param("userId") int userId, @Param("username") String username);

    @Modifying
    @Transactional
    @Query(value = "UPDATE faq_approval fa INNER JOIN faq f ON fa.faq_id = f.id SET fa.hidden_by = NULL, fa.hidden_by_author = false WHERE f.author = :userId AND fa.hidden_by_author = true", nativeQuery = true)
    void unhideAuthorHiddenFaqsByAuthorId(@Param("userId") int userId);
    @Query(value = """
    SELECT
        SUM(CASE WHEN appr.approved IS NULL THEN 1 ELSE 0 END) as countPending,
        SUM(CASE WHEN appr.approved = FALSE THEN 1 ELSE 0 END) as countDenied
    FROM faq fa
    LEFT JOIN faq_approval appr ON appr.faq_id = fa.id
    WHERE fa.author = :userId AND (appr.hidden_by IS NULL)
    LIMIT 1
    """, nativeQuery = true)
    List<Object[]> getApprovalCountsForUser(@Param("userId") int userId);

}
