package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AgencyRepository extends CrudRepository<AgencyEntity, Integer> {

    @Query(value = "SELECT COUNT(*) FROM agency ag WHERE ag.approved = TRUE", nativeQuery = true)
    int countApproved();

    @Query(value = "SELECT COUNT(*) FROM agency ag WHERE ISNULL(ag.approved) OR ag.approved = FALSE", nativeQuery = true)
    int countPendingOrDenied();

    @Query(value = """
        SELECT
            ag.id,
            ag.name,
            ag.mission_statement,
            ag.website_url,
            ag.approved,
            approved_by.username AS approved_by_username,
            lo.id AS location_id,
            lo.addr_line1,
            lo.addr_line2,
            lo.city,
            lo.state,
            lo.zip_code,
            agent.id AS agent_info_id,
            agent.email AS agent_info_email,
            agent.phone_number AS agent_info_phone_number,
            agent.id AS user_id,
            agent.first_name,
            agent.last_name,
            agent.username,
            agent.profile_picture_url,
            agent.verified_foster,
            agent.faq_author,
            agent.verified_agency_rep,
            agent.created_at
        FROM agency ag
        INNER JOIN user agent ON ag.agent = agent.id
        INNER JOIN location lo ON ag.address = lo.id
        INNER JOIN user approved_by ON ag.approved_by_id = approved_by.id
        WHERE ag.approved = TRUE;
    """, nativeQuery = true)
    List<Object[]> allApprovedAgencies(Pageable pageable);
    @Query(value = """
SELECT
    ag.id,
    ag.name,
    ag.mission_statement,
    ag.website_url,
    ag.approved,
    IFNULL(approved_by.username, '') AS approved_by_username,
    lo.id AS location_id,
    lo.addr_line1,
    lo.addr_line2,
    lo.city,
    lo.state,
    lo.zip_code,
    agent.id AS agent_info_id,
    agent.email AS agent_info_email,
    agent.phone_number AS agent_info_phone_number,
    agent.id AS user_id,
    agent.first_name,
    agent.last_name,
    agent.username,
    agent.profile_picture_url,
    agent.verified_foster,
    agent.faq_author,
    agent.verified_agency_rep,
    agent.created_at
FROM agency ag
INNER JOIN user agent ON ag.agent = agent.id
INNER JOIN location lo ON ag.address = lo.id
LEFT JOIN user approved_by ON ag.approved_by_id = approved_by.id
WHERE ISNULL(ag.approved) OR ag.approved = FALSE;
    """, nativeQuery = true)
    List<Object[]> allPendingAgencies(Pageable pageable);

    Long countByApprovedNull();

    @Query("SELECT a FROM AgencyEntity a WHERE a.agent.id = :agentId")
    List<AgencyEntity> findByAgentId(@Param("agentId") int agentId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM agency_deletion_request WHERE agency = :agencyId", nativeQuery = true)
    void deleteDeletionRequestsByAgencyId(@Param("agencyId") int agencyId);

}
