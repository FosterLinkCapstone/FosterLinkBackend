package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyDeletionRequestResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
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

    public List<AgencyResponse> getAllApprovedAgencies(int pageNumber, boolean includeDeletionRequestForAdmin, Integer currentUserId) {
        List<AgencyResponse> agencies = new ArrayList<>();
        List<Object[]> toMap = agencyRepository.allApprovedAgencies(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return getAgencyResponses(agencies, toMap, false, includeDeletionRequestForAdmin, currentUserId);
    }

    /** Search by: agency (name, mission), agent (full name, username, email, phone), location (city, state, zip). */
    public List<AgencyResponse> getAllApprovedAgenciesWithSearch(int pageNumber, String search, String searchBy, boolean includeDeletionRequestForAdmin, Integer currentUserId) {
        List<AgencyResponse> agencies = new ArrayList<>();
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String normalizedSearchBy = (searchBy != null && !searchBy.isBlank()) ? searchBy.trim() : null;
        if (normalizedSearch == null || normalizedSearch.isEmpty()) {
            return getAllApprovedAgencies(pageNumber, includeDeletionRequestForAdmin, currentUserId);
        }
        List<Object[]> toMap = agencyRepository.allApprovedAgenciesWithSearch(normalizedSearch, normalizedSearchBy, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return getAgencyResponses(agencies, toMap, false, includeDeletionRequestForAdmin, currentUserId);
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
            agency.setLocation(location);

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

    private static java.util.Date toDate(Object o) {
        if (o == null) return null;
        if (o instanceof java.util.Date) return (java.util.Date) o;
        if (o instanceof java.sql.Timestamp) return new java.util.Date(((java.sql.Timestamp) o).getTime());
        return null;
    }

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

            LocationEntity locationEntity = new LocationEntity();
            locationEntity.setId((Integer)obj[8]);
            locationEntity.setAddrLine1((String)obj[9]);
            locationEntity.setAddrLine2((String)obj[10]);
            locationEntity.setCity((String)obj[11]);
            locationEntity.setState((String)obj[12]);
            locationEntity.setZipCode((Integer)obj[13]);
            agency.setLocation(locationEntity);

            AgentInfoResponse agentInfoResponse = new AgentInfoResponse();
            agentInfoResponse.setId((Integer)obj[14]);
            agentInfoResponse.setEmail((String)obj[15]);
            agentInfoResponse.setPhoneNumber((String)obj[16]);
            agency.setAgentInfo(agentInfoResponse);

            agency.setAgent(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 17, 17 + 11)));

            if (includeHiddenBy && obj.length > 28) {
                agency.setHiddenByUsername((String) obj[28]);
            }

            boolean includeDeletionRequest = includeDeletionRequestForAdmin
                || (currentUserId != null && currentUserId.equals(obj[17]));
            if (includeDeletionRequest && obj.length > 30) {
                agency.setDeletionRequestedAt(toDate(obj[28]));
                agency.setDeletionRequestedByUsername(obj[29] != null ? (String) obj[29] : null);
                agency.setDeletionRequestId(obj[30] != null ? (Integer) obj[30] : null);
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
        agency.setCreatedAt(e.getCreatedAt());
        agency.setUpdatedAt(e.getUpdatedAt());
        agency.setLocation(e.getAddress());
        agency.setAgent(new UserResponse(e.getAgent()));
        agency.setAgentInfo(new AgentInfoResponse(e.getAgent()));
        return agency;
    }

}
