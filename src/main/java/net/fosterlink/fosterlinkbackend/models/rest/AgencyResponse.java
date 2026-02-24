package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;

@Data
@Schema(description = "Details about an agency",
        requiredProperties = {"id", "agencyName", "agencyMissionStatement", "agencyWebsiteLink", "location", "agent", "agentInfo"})
public class AgencyResponse {

    @Schema(description = "The internal ID of the agency")
    private int id;
    @Schema(description = "The name of the agency")
    private String agencyName;
    @Schema(description = "The mission statement of the agency")
    private String agencyMissionStatement;
    @Schema(description = "The website URL of the agency")
    private String agencyWebsiteLink;
    @Schema(description = "The location/address of the agency")
    private LocationEntity location;
    @Schema(description = "The user who represents this agency")
    private UserResponse agent;
    @Schema(description = "Contact information for the agency representative")
    private AgentInfoResponse agentInfo;
    @Schema(description = "Approval status: 0 = pending, 1 = approved, 2 = denied")
    private int approved;
    @Schema(description = "The username of the administrator who approved/denied the agency. Can be null if pending.")
    private String approvedByUsername;

    @Schema(description = "The username of the administrator who hid this agency. Null if not hidden.")
    private String hiddenByUsername;

    @Schema(description = "If the owner has requested deletion, the date the request was created. Null otherwise.")
    private java.util.Date deletionRequestedAt;

    @Schema(description = "If the owner has requested deletion, the username of the requester. Null otherwise.")
    private String deletionRequestedByUsername;

    @Schema(description = "If the owner has requested deletion, the ID of the pending deletion request. Null otherwise.")
    private Integer deletionRequestId;

}
