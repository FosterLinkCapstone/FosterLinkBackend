package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyDeletionRequestResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
import net.fosterlink.fosterlinkbackend.models.rest.LocationResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PublicAgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PublicUserResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.repositories.AgencyDeletionRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class AgencyMapper {

    private @Autowired AgencyRepository agencyRepository;
    private @Autowired AgencyDeletionRequestRepository agencyDeletionRequestRepository;
    private @Autowired UserMapper userMapper;

    public List<PublicAgencyResponse> getAllApprovedAgencies(int pageNumber, boolean includeDeletionRequestForAdmin, Integer currentUserId) {
        List<Object[]> toMap = agencyRepository.allApprovedAgencies(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return toPublicAgencyResponses(toMap);
    }

    /** Search by: agency (name, mission), agent (full name, username, email, phone), location (city, state, zip). */
    public List<PublicAgencyResponse> getAllApprovedAgenciesWithSearch(int pageNumber, String search, String searchBy, boolean includeDeletionRequestForAdmin, Integer currentUserId) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String normalizedSearchBy = (searchBy != null && !searchBy.isBlank()) ? searchBy.trim() : null;
        if (normalizedSearch == null || normalizedSearch.isEmpty()) {
            return getAllApprovedAgencies(pageNumber, includeDeletionRequestForAdmin, currentUserId);
        }
        List<Object[]> toMap = agencyRepository.allApprovedAgenciesWithSearch(normalizedSearch, normalizedSearchBy, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return toPublicAgencyResponses(toMap);
    }

    public int countApprovedWithSearch(String search, String searchBy) {
        if (search == null || search.isBlank()) {
            return agencyRepository.countApproved();
        }
        String normalizedSearch = search.trim();
        String normalizedSearchBy = (searchBy != null && !searchBy.isBlank()) ? searchBy.trim() : "agency";
        return agencyRepository.countApprovedWithSearch(normalizedSearch, normalizedSearchBy);
    }

    public List<AgencyResponse> getAllPendingAgencies(int pageNumber) {
        List<AgencyResponse> agencies = new ArrayList<>();
        List<Object[]> toMap = agencyRepository.allPendingAgencies(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return getAgencyResponses(agencies, toMap, false, false, null);
    }

    public List<AgencyResponse> getAllHiddenAgencies(int pageNumber) {
        List<AgencyResponse> agencies = new ArrayList<>();
        List<Object[]> toMap = agencyRepository.allHiddenAgencies(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return getAgencyResponses(agencies, toMap, true, false, null);
    }

    public List<AgencyDeletionRequestResponse> getAllDeletionRequests(int pageNumber, String sortBy) {
        PageRequest page = PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE);
        List<Object[]> rows = "urgency".equals(sortBy)
                ? agencyDeletionRequestRepository.findAllPendingByUrgency(page)
                : agencyDeletionRequestRepository.findAllPendingByRecency(page);
        List<AgencyDeletionRequestResponse> results = new ArrayList<>();
        for (Object[] obj : rows) {
            AgencyDeletionRequestResponse dr = new AgencyDeletionRequestResponse();
            dr.setId((Integer) obj[0]);
            dr.setCreatedAt(toDate(obj[1]));
            dr.setAutoApproveBy(toDate(obj[2]));
            dr.setAutoApproved(obj[3] != null && (Boolean) obj[3]);
            dr.setDelayNote((String) obj[4]);

            AgencyResponse agency = new AgencyResponse();
            agency.setId((Integer) obj[5]);
            agency.setAgencyName((String) obj[6]);
            agency.setAgencyMissionStatement((String) obj[7]);
            agency.setAgencyWebsiteLink((String) obj[8]);
            agency.setApproved(obj[9] == null ? 1 : (((Boolean) obj[9]) ? 2 : 3));
            agency.setHiddenByUsername((String) obj[11]);
            agency.setApprovedByUsername(obj[12] == null ? null : (String) obj[12]);

            LocationEntity location = new LocationEntity();
            location.setId((Integer) obj[13]);
            location.setAddrLine1((String) obj[14]);
            location.setAddrLine2((String) obj[15]);
            location.setCity((String) obj[16]);
            location.setState((String) obj[17]);
            location.setZipCode((Integer) obj[18]);
            agency.setLocation(new LocationResponse(location));

            AgentInfoResponse agentInfo = new AgentInfoResponse();
            agentInfo.setId((Integer) obj[19]);
            agentInfo.setEmail((String) obj[20]);
            agentInfo.setPhoneNumber((String) obj[21]);
            agency.setAgentInfo(agentInfo);

            agency.setAgent(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 22, 33)));
            dr.setAgency(agency);

            UserResponse requester = new UserResponse();
            requester.setId((Integer) obj[33]);
            requester.setFullName(obj[34] + " " + obj[35]);
            requester.setUsername((String) obj[36]);
            requester.setProfilePictureUrl((String) obj[37]);
            requester.setVerified(
                    (Boolean) obj[38] || (Boolean) obj[39] || (Boolean) obj[40]
            );
            requester.setCreatedAt(toDate(obj[41]));
            dr.setRequestedBy(requester);

            if (obj[42] != null) {
                dr.setReviewedBy(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 42, 53)));
            }

            results.add(dr);
        }
        return results;
    }

    /**
     * Builds public-safe agency responses. Agent contact info (email, phone) is included
     * only when the agent has opted in via show_contact_info (obj[8]).
     *
     * Column layout (0-indexed, matches allApprovedAgencies / allApprovedAgenciesWithSearch):
     *  0  ag.id               8  ag.show_contact_info   16 agent_info_email
     *  1  ag.name             9  lo.id                  17 agent_info_phone
     *  2  mission_statement  10  lo.addr_line1          18 agent.id (user_id)
     *  3  ag.website_url     11  lo.addr_line2          19 agent.first_name
     *  4  ag.approved        12  lo.city                20 agent.last_name
     *  5  approved_by.user   13  lo.state               21 agent.username
     *  6  ag.created_at      14  lo.zip_code            22 agent.profile_picture_url
     *  7  ag.updated_at      15  agent_info_id          23 agent.verified_foster
     *                                                   24 agent.faq_author
     *                                                   25 agent.verified_agency_rep
     *                                                   26 agent.created_at
     *                                                   27 agent.banned_at
     *                                                   28 agent.restricted_at
     */
    private List<PublicAgencyResponse> toPublicAgencyResponses(List<Object[]> toMap) {
        List<PublicAgencyResponse> results = new ArrayList<>();
        for (Object[] obj : toMap) {
            PublicAgencyResponse agency = new PublicAgencyResponse();
            agency.setId((Integer) obj[0]);
            agency.setAgencyName((String) obj[1]);
            agency.setAgencyMissionStatement((String) obj[2]);
            agency.setAgencyWebsiteLink((String) obj[3]);
            agency.setApproved(obj[4] == null ? 1 : (((Boolean) obj[4]) ? 2 : 3));
            agency.setCreatedAt(toDate(obj[6]));
            agency.setUpdatedAt(toDate(obj[7]));

            boolean showContactInfo = obj[8] != null && (Boolean) obj[8];
            agency.setShowContactInfo(showContactInfo);

            LocationEntity locationEntity = new LocationEntity();
            locationEntity.setId((Integer) obj[9]);
            locationEntity.setAddrLine1((String) obj[10]);
            locationEntity.setAddrLine2((String) obj[11]);
            locationEntity.setCity((String) obj[12]);
            locationEntity.setState((String) obj[13]);
            locationEntity.setZipCode((Integer) obj[14]);
            agency.setLocation(new LocationResponse(locationEntity));

            // obj[15] = agent_info_id, obj[16] = email, obj[17] = phone — exposed only when opted in
            if (showContactInfo) {
                agency.setAgentEmail((String) obj[16]);
                agency.setAgentPhoneNumber((String) obj[17]);
            }

            agency.setAgent(userMapper.mapPublicUserResponse(Arrays.copyOfRange(obj, 18, 29)));

            results.add(agency);
        }
        return results;
    }

    private static java.util.Date toDate(Object o) {
        if (o == null) return null;
        if (o instanceof java.util.Date) return (java.util.Date) o;
        if (o instanceof java.sql.Timestamp) return new java.util.Date(((java.sql.Timestamp) o).getTime());
        return null;
    }

    /**
     * Builds admin AgencyResponse rows. Column layout matches allPendingAgencies / allHiddenAgencies
     * (both share the same structure; allHiddenAgencies appends hidden_by_username at the end):
     *  0  ag.id               8  ag.show_contact_info   16 agent_info_email
     *  1  ag.name             9  lo.id                  17 agent_info_phone
     *  2  mission_statement  10  lo.addr_line1          18 agent.id (user_id)
     *  3  ag.website_url     11  lo.addr_line2          19 agent.first_name
     *  4  ag.approved        12  lo.city                20 agent.last_name
     *  5  approved_by.user   13  lo.state               21 agent.username
     *  6  ag.created_at      14  lo.zip_code            22 agent.profile_picture_url
     *  7  ag.updated_at      15  agent_info_id          23 agent.verified_foster
     *                                                   24 agent.faq_author
     *                                                   25 agent.verified_agency_rep
     *                                                   26 agent.created_at
     *                                                   27 agent.banned_at
     *                                                   28 agent.restricted_at
     * allHiddenAgencies only: 29 hidden_by_username
     */
    private List<AgencyResponse> getAgencyResponses(List<AgencyResponse> agencies, List<Object[]> toMap, boolean includeHiddenBy, boolean includeDeletionRequestForAdmin, Integer currentUserId) {
        for (Object[] obj : toMap) {
            AgencyResponse agency = new AgencyResponse();
            agency.setId((Integer)obj[0]);
            agency.setAgencyName((String)obj[1]);
            agency.setAgencyMissionStatement((String)obj[2]);
            agency.setAgencyWebsiteLink((String)obj[3]);
            agency.setApproved(obj[4] == null ? 1 : (((Boolean) obj[4]) ? 2 : 3));
            agency.setApprovedByUsername(obj[5] == null ? null : (String) obj[5]);
            agency.setCreatedAt(toDate(obj[6]));
            agency.setUpdatedAt(toDate(obj[7]));

            agency.setShowContactInfo(obj[8] != null && (Boolean) obj[8]);

            LocationEntity locationEntity = new LocationEntity();
            locationEntity.setId((Integer)obj[9]);
            locationEntity.setAddrLine1((String)obj[10]);
            locationEntity.setAddrLine2((String)obj[11]);
            locationEntity.setCity((String)obj[12]);
            locationEntity.setState((String)obj[13]);
            locationEntity.setZipCode((Integer)obj[14]);
            agency.setLocation(new LocationResponse(locationEntity));

            AgentInfoResponse agentInfoResponse = new AgentInfoResponse();
            agentInfoResponse.setId((Integer)obj[15]);
            agentInfoResponse.setEmail((String)obj[16]);
            agentInfoResponse.setPhoneNumber((String)obj[17]);
            agency.setAgentInfo(agentInfoResponse);

            agency.setAgent(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 18, 29)));

            if (includeHiddenBy && obj.length > 29) {
                agency.setHiddenByUsername((String) obj[29]);
            }

            boolean includeDeletionRequest = includeDeletionRequestForAdmin
                || (currentUserId != null && currentUserId.equals(obj[18]));
            if (includeDeletionRequest && obj.length > 31) {
                agency.setDeletionRequestedAt(toDate(obj[29]));
                agency.setDeletionRequestedByUsername(obj[30] != null ? (String) obj[30] : null);
                agency.setDeletionRequestId(obj[31] != null ? (Integer) obj[31] : null);
            }

            agencies.add(agency);
        }
        return agencies;
    }

    /**
     * Builds an AgencyResponse from an entity (e.g. for admin list of agencies by user).
     * Uses approved: 0 = pending, 1 = approved, 2 = denied. Approver username is not set.
     *
     * IMPORTANT: AgencyEntity.address and AgencyEntity.agent are LAZY associations.
     * Callers MUST load the entity via AgencyRepository.findByAgentIdWithRelations (which JOIN FETCHes
     * both associations) rather than findByAgentId, to avoid LazyInitializationException.
     */
    public AgencyResponse fromEntity(AgencyEntity e) {
        AgencyResponse agency = new AgencyResponse();
        agency.setId(e.getId());
        agency.setAgencyName(e.getName());
        agency.setAgencyMissionStatement(e.getMissionStatement());
        agency.setAgencyWebsiteLink(e.getWebsiteUrl());
        agency.setApproved(e.getApproved() == null ? 0 : (e.getApproved() ? 1 : 2));
        agency.setApprovedByUsername(null);
        agency.setShowContactInfo(e.isShowContactInfo());
        agency.setCreatedAt(e.getCreatedAt());
        agency.setUpdatedAt(e.getUpdatedAt());
        agency.setLocation(new LocationResponse(e.getAddress()));
        agency.setAgent(new UserResponse(e.getAgent()));
        agency.setAgentInfo(new AgentInfoResponse(e.getAgent()));
        return agency;
    }

}
