package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AgencyRepository extends CrudRepository<AgencyEntity, Integer> {

    @Query(value = "SELECT COUNT(*) FROM agency ag WHERE ag.approved = TRUE AND ag.hidden = FALSE", nativeQuery = true)
    int countApproved();

    @Query(value = """
        SELECT COUNT(*) FROM agency ag
        INNER JOIN user agent ON ag.agent = agent.id
        INNER JOIN location lo ON ag.address = lo.id
        INNER JOIN user approved_by ON ag.approved_by_id = approved_by.id
        WHERE ag.approved = TRUE AND ag.hidden = FALSE
        AND (:search IS NULL OR :search = '' OR (
            (:searchBy = 'agency' AND (LOWER(IFNULL(ag.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(IFNULL(ag.mission_statement, '')) LIKE LOWER(CONCAT('%', :search, '%'))))
            OR (:searchBy = 'agent' AND (LOWER(CONCAT(IFNULL(agent.first_name, ''), ' ', IFNULL(agent.last_name, ''))) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(IFNULL(agent.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(IFNULL(agent.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(IFNULL(agent.phone_number, '')) LIKE LOWER(CONCAT('%', :search, '%'))))
            OR (:searchBy = 'location' AND (LOWER(IFNULL(lo.city, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(IFNULL(lo.state, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR CAST(lo.zip_code AS CHAR) LIKE CONCAT('%', :search, '%')))
        ))
        """, nativeQuery = true)
    int countApprovedWithSearch(@Param("search") String search, @Param("searchBy") String searchBy);

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
            ag.created_at,
            ag.updated_at,
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
            agent.created_at,
            agent.banned_at,
            agent.restricted_at,
            (SELECT dr.created_at FROM agency_deletion_request dr WHERE dr.agency = ag.id AND dr.approved IS NULL LIMIT 1) AS deletion_requested_at,
            (SELECT u.username FROM agency_deletion_request dr INNER JOIN user u ON dr.requested_by = u.id WHERE dr.agency = ag.id AND dr.approved IS NULL LIMIT 1) AS deletion_requested_by_username,
            (SELECT dr.id FROM agency_deletion_request dr WHERE dr.agency = ag.id AND dr.approved IS NULL LIMIT 1) AS deletion_request_id
        FROM agency ag
        INNER JOIN user agent ON ag.agent = agent.id
        INNER JOIN location lo ON ag.address = lo.id
        INNER JOIN user approved_by ON ag.approved_by_id = approved_by.id
        WHERE ag.approved = TRUE AND ag.hidden = FALSE;
    """, nativeQuery = true)
    @Cacheable(value = "agencyApprovedRows", key = "#pageable.pageNumber")
    List<Object[]> allApprovedAgencies(Pageable pageable);

    @Query(value = """
        SELECT
            ag.id,
            ag.name,
            ag.mission_statement,
            ag.website_url,
            ag.approved,
            approved_by.username AS approved_by_username,
            ag.created_at,
            ag.updated_at,
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
            agent.created_at,
            agent.banned_at,
            agent.restricted_at,
            (SELECT dr.created_at FROM agency_deletion_request dr WHERE dr.agency = ag.id AND dr.approved IS NULL LIMIT 1) AS deletion_requested_at,
            (SELECT u.username FROM agency_deletion_request dr INNER JOIN user u ON dr.requested_by = u.id WHERE dr.agency = ag.id AND dr.approved IS NULL LIMIT 1) AS deletion_requested_by_username,
            (SELECT dr.id FROM agency_deletion_request dr WHERE dr.agency = ag.id AND dr.approved IS NULL LIMIT 1) AS deletion_request_id
        FROM agency ag
        INNER JOIN user agent ON ag.agent = agent.id
        INNER JOIN location lo ON ag.address = lo.id
        INNER JOIN user approved_by ON ag.approved_by_id = approved_by.id
        WHERE ag.approved = TRUE AND ag.hidden = FALSE
        AND (:search IS NULL OR :search = '' OR (
            (:searchBy = 'agency' AND (LOWER(IFNULL(ag.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(IFNULL(ag.mission_statement, '')) LIKE LOWER(CONCAT('%', :search, '%'))))
            OR (:searchBy = 'agent' AND (LOWER(CONCAT(IFNULL(agent.first_name, ''), ' ', IFNULL(agent.last_name, ''))) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(IFNULL(agent.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(IFNULL(agent.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(IFNULL(agent.phone_number, '')) LIKE LOWER(CONCAT('%', :search, '%'))))
            OR (:searchBy = 'location' AND (LOWER(IFNULL(lo.city, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(IFNULL(lo.state, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR CAST(lo.zip_code AS CHAR) LIKE CONCAT('%', :search, '%')))
        ))
        """, nativeQuery = true)
    List<Object[]> allApprovedAgenciesWithSearch(@Param("search") String search, @Param("searchBy") String searchBy, Pageable pageable);
    @Query(value = """
SELECT
    ag.id,
    ag.name,
    ag.mission_statement,
    ag.website_url,
    ag.approved,
    IFNULL(approved_by.username, '') AS approved_by_username,
    ag.created_at,
    ag.updated_at,
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
    agent.created_at,
    agent.banned_at,
    agent.restricted_at
FROM agency ag
INNER JOIN user agent ON ag.agent = agent.id
INNER JOIN location lo ON ag.address = lo.id
LEFT JOIN user approved_by ON ag.approved_by_id = approved_by.id
WHERE ISNULL(ag.approved) OR ag.approved = FALSE;
    """, nativeQuery = true)
    List<Object[]> allPendingAgencies(Pageable pageable);


    @Query(value = """
        SELECT COUNT(*)
        FROM agency ag
        WHERE ag.approved = FALSE
          AND (ag.hidden = FALSE OR ag.hidden IS NULL)
          AND NOT EXISTS (
              SELECT 1 FROM agency_deletion_request dr
              WHERE dr.agency = ag.id AND dr.approved IS NULL
          )
        """, nativeQuery = true)
    Long countPending();

    @Query(value = "SELECT COUNT(*) FROM agency ag WHERE ag.hidden = TRUE", nativeQuery = true)
    int countHidden();

    @Query(value = """
        SELECT
            ag.id,
            ag.name,
            ag.mission_statement,
            ag.website_url,
            ag.approved,
            IFNULL(approved_by.username, '') AS approved_by_username,
            ag.created_at,
            ag.updated_at,
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
            agent.created_at,
            agent.banned_at,
            agent.restricted_at,
            (SELECT u.username FROM `user` u WHERE u.id = ag.hidden_by_user_id) AS hidden_by_username
        FROM agency ag
        INNER JOIN user agent ON ag.agent = agent.id
        INNER JOIN location lo ON ag.address = lo.id
        LEFT JOIN user approved_by ON ag.approved_by_id = approved_by.id
        WHERE ag.hidden = TRUE;
    """, nativeQuery = true)
    List<Object[]> allHiddenAgencies(Pageable pageable);

    @Query("SELECT a FROM AgencyEntity a WHERE a.agent.id = :agentId")
    List<AgencyEntity> findByAgentId(@Param("agentId") int agentId);

    @Query("SELECT a FROM AgencyEntity a JOIN FETCH a.address JOIN FETCH a.agent WHERE a.agent.id = :agentId")
    List<AgencyEntity> findByAgentIdWithRelations(@Param("agentId") int agentId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE agency SET hidden = true, hidden_by_deletion_request = 1 WHERE agent = :userId AND hidden = false", nativeQuery = true)
    void hideVisibleAgenciesByAgentId(@Param("userId") int userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE agency SET hidden = false, hidden_by_deletion_request = 0 WHERE agent = :userId AND hidden_by_deletion_request = 1", nativeQuery = true)
    void unhidePendingDeletionAgenciesByAgentId(@Param("userId") int userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM agency_deletion_request WHERE agency = :agencyId", nativeQuery = true)
    void deleteDeletionRequestsByAgencyId(@Param("agencyId") int agencyId);

    /** Deletes the agency row by ID. Use after deleting dependents (e.g. deletion requests). Then delete location. */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM agency WHERE id = :id", nativeQuery = true)
    void deleteAgencyById(@Param("id") int id);

    /** Sets an agency back to pending (clears approval so it no longer appears in the approved list). */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE agency SET approved = NULL, approved_by_id = NULL, updated_at = :updatedAt WHERE id = :id", nativeQuery = true)
    void setAgencyPending(@Param("id") int id, @Param("updatedAt") java.util.Date updatedAt);

}
