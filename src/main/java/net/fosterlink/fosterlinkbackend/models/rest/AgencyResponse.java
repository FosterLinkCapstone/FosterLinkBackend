package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.Data;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;

@Data
public class AgencyResponse {

    private int id;
    private String agencyName;
    private String agencyMissionStatement;
    private String agencyWebsiteLink;
    private LocationEntity location;
    private UserResponse agent;
    private AgentInfoResponse agentInfo;
    private int approved;
    private String approvedByUsername;

}
