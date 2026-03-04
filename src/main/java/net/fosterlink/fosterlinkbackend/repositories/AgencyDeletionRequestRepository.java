package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.AgencyDeletionRequestEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AgencyDeletionRequestRepository extends CrudRepository<AgencyDeletionRequestEntity, Integer> {

    @Query(value = """
        SELECT
            dr.id,
            dr.created_at,
            dr.approved,
            ag.id AS agency_id,
            ag.name,
            ag.mission_statement,
            ag.website_url,
            ag.approved AS agency_approved,
            ag.hidden,
            ag.hidden_by_username,
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
            agent.created_at AS agent_created_at,
            requester.id AS requester_id,
            requester.first_name AS requester_first_name,
            requester.last_name AS requester_last_name,
            requester.username AS requester_username,
            requester.profile_picture_url AS requester_pic,
            requester.verified_foster AS requester_vf,
            requester.faq_author AS requester_fa,
            requester.verified_agency_rep AS requester_var,
            requester.created_at AS requester_created_at
        FROM agency_deletion_request dr
        INNER JOIN agency ag ON dr.agency = ag.id
        INNER JOIN location lo ON ag.address = lo.id
        LEFT JOIN user approved_by ON ag.approved_by_id = approved_by.id
        INNER JOIN user agent ON ag.agent = agent.id
        INNER JOIN user requester ON dr.requested_by = requester.id
        WHERE dr.approved IS NULL
        ORDER BY dr.created_at ASC
    """, nativeQuery = true)
    List<Object[]> findAllPendingPaginated(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM agency_deletion_request WHERE approved IS NULL", nativeQuery = true)
    int countPending();

    @Query("SELECT dr FROM AgencyDeletionRequestEntity dr JOIN FETCH dr.requestedBy WHERE dr.agency.id = :agencyId AND dr.approved IS NULL")
    Optional<AgencyDeletionRequestEntity> findPendingByAgencyId(@Param("agencyId") int agencyId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM agency_deletion_request WHERE agency = :agencyId", nativeQuery = true)
    void deleteByAgencyId(@Param("agencyId") int agencyId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM agency_deletion_request WHERE id = :id", nativeQuery = true)
    void deleteRequestById(@Param("id") int id);

    @Modifying
    @Transactional
    @Query(value = "DELETE adr FROM agency_deletion_request adr INNER JOIN agency a ON adr.agency = a.id WHERE a.agent = :userId", nativeQuery = true)
    void deleteByAgencyAgentId(@Param("userId") int userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE adr FROM agency_deletion_request adr INNER JOIN agency a ON adr.agency = a.id WHERE a.agent = :userId AND adr.approved IS NULL", nativeQuery = true)
    void deletePendingByAgencyAgentId(@Param("userId") int userId);
}
