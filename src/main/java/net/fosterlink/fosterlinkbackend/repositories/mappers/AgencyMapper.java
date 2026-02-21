package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AgencyMapper {

    private @Autowired AgencyRepository agencyRepository;
    private @Autowired UserMapper userMapper;

    public List<AgencyResponse> getAllApprovedAgencies(int pageNumber) {
        List<AgencyResponse> agencies = new ArrayList<>();
        List<Object[]> toMap = agencyRepository.allApprovedAgencies(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return getAgencyResponses(agencies, toMap);
    }
    public List<AgencyResponse> getAllPendingAgencies(int pageNumber) {
        List<AgencyResponse> agencies = new ArrayList<>();
        List<Object[]> toMap = agencyRepository.allPendingAgencies(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return getAgencyResponses(agencies, toMap);
    }

    private List<AgencyResponse> getAgencyResponses(List<AgencyResponse> agencies, List<Object[]> toMap) {
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

            agency.setAgent(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 15, obj.length+1)));
            agencies.add(agency);
        }
        return agencies;
    }

}
