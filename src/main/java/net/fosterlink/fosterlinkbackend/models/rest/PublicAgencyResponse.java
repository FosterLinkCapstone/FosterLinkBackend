package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Public-safe agency DTO. Omits:
 * <ul>
 *   <li>approvedByUsername, hiddenByUsername — admin-workflow metadata, resolves 04/F-04</li>
 *   <li>deletionRequestedAt, deletionRequestedByUsername, deletionRequestId — admin-workflow metadata</li>
 * </ul>
 * Agent contact info (email, phone) is included only when the agent has opted in via
 * {@code showContactInfo}. When {@code showContactInfo} is false, {@code agentEmail} and
 * {@code agentPhoneNumber} are null (resolves 04/F-03).
 * Also uses {@link LocationResponse} instead of the raw {@link net.fosterlink.fosterlinkbackend.entities.LocationEntity}
 * JPA entity to avoid leaking the internal database ID (resolves 04/F-07).
 * Uses {@link PublicUserResponse} for the agent field to omit banned/restricted flags (resolves 04/F-06).
 */
@Data
@Schema(description = "Public-safe agency details. Agent contact info is included only when the agent has opted in.")
public class PublicAgencyResponse {

    @Schema(description = "The internal ID of the agency")
    private int id;

    @Schema(description = "The name of the agency")
    private String agencyName;

    @Schema(description = "The mission statement of the agency")
    private String agencyMissionStatement;

    @Schema(description = "The website URL of the agency")
    private String agencyWebsiteLink;

    @Schema(description = "The location/address of the agency")
    private LocationResponse location;

    @Schema(description = "Basic public information about the user who represents this agency")
    private PublicUserResponse agent;

    @Schema(description = "Approval status: 0 = pending, 1 = approved, 2 = denied")
    private int approved;

    @Schema(description = "Whether the agent has opted in to showing their contact info publicly")
    private boolean showContactInfo;

    @Schema(description = "The agent's email address. Null unless showContactInfo is true.")
    private String agentEmail;

    @Schema(description = "The agent's phone number. Null unless showContactInfo is true.")
    private String agentPhoneNumber;

    @Schema(description = "When the agency was created")
    private java.util.Date createdAt;

    @Schema(description = "When the agency was last updated. Null if never updated.")
    private java.util.Date updatedAt;
}
