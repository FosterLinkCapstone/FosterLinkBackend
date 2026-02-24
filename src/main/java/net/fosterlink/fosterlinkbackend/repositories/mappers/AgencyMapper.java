package net.fosterlink.fosterlinkbackend.repositories.mappers;

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

    public List<AgencyDeletionRequestResponse> getAllDeletionRequests(int pageNumber) {
        List<Object[]> rows = agencyDeletionRequestRepository.findAllPendingPaginated(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        List<AgencyDeletionRequestResponse> results = new ArrayList<>();
        for (Object[] obj : rows) {
            AgencyDeletionRequestResponse dr = new AgencyDeletionRequestResponse();
            dr.setId((Integer) obj[0]);
            dr.setCreatedAt((Date) obj[1]);

            AgencyResponse agency = new AgencyResponse();
            agency.setId((Integer) obj[3]);
            agency.setAgencyName((String) obj[4]);
            agency.setAgencyMissionStatement((String) obj[5]);
            agency.setAgencyWebsiteLink((String) obj[6]);
            agency.setApproved(obj[7] == null ? 1 : (((Boolean) obj[7]) ? 2 : 3));
            agency.setHiddenByUsername((String) obj[9]);
            agency.setApprovedByUsername(obj[10] == null ? null : (String) obj[10]);

            LocationEntity location = new LocationEntity();
            location.setId((Integer) obj[11]);
            location.setAddrLine1((String) obj[12]);
            location.setAddrLine2((String) obj[13]);
            location.setCity((String) obj[14]);
            location.setState((String) obj[15]);
            location.setZipCode((Integer) obj[16]);
            agency.setLocation(location);

            AgentInfoResponse agentInfo = new AgentInfoResponse();
            agentInfo.setId((Integer) obj[17]);
            agentInfo.setEmail((String) obj[18]);
            agentInfo.setPhoneNumber((String) obj[19]);
            agency.setAgentInfo(agentInfo);

            agency.setAgent(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 20, 29)));
            dr.setAgency(agency);

            UserResponse requester = new UserResponse();
            requester.setId((Integer) obj[29]);
            requester.setFullName(obj[30] + " " + obj[31]);
            requester.setUsername((String) obj[32]);
            requester.setProfilePictureUrl((String) obj[33]);
            requester.setVerified(
                    (Boolean) obj[34] || (Boolean) obj[35] || (Boolean) obj[36]
            );
            requester.setCreatedAt((Date) obj[37]);
            dr.setRequestedBy(requester);

            results.add(dr);
        }
        return results;
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

            LocationEntity locationEntity = new LocationEntity();
            locationEntity.setId((Integer)obj[6]);
            locationEntity.setAddrLine1((String)obj[7]);
            locationEntity.setAddrLine2((String)obj[8]);
            locationEntity.setCity((String)obj[9]);
            locationEntity.setState((String)obj[10]);
            locationEntity.setZipCode((Integer)obj[11]);
            agency.setLocation(locationEntity);

            AgentInfoResponse agentInfoResponse = new AgentInfoResponse();
            agentInfoResponse.setId((Integer)obj[12]);
            agentInfoResponse.setEmail((String)obj[13]);
            agentInfoResponse.setPhoneNumber((String)obj[14]);
            agency.setAgentInfo(agentInfoResponse);

            agency.setAgent(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 15, 15 + 9)));

            if (includeHiddenBy && obj.length > 24) {
                agency.setHiddenByUsername((String) obj[24]);
            }

            boolean includeDeletionRequest = includeDeletionRequestForAdmin
                || (currentUserId != null && currentUserId.equals(obj[15]));
            if (includeDeletionRequest && obj.length > 25) {
                agency.setDeletionRequestedAt(obj[24] != null ? (Date) obj[24] : null);
                agency.setDeletionRequestedByUsername(obj[25] != null ? (String) obj[25] : null);
                agency.setDeletionRequestId(obj.length > 26 && obj[26] != null ? (Integer) obj[26] : null);
            }

            agencies.add(agency);
        }
        return agencies;
    }

}
