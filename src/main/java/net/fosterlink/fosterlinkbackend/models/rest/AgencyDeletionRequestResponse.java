package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "A request by an agency owner to have their agency permanently deleted")
public class AgencyDeletionRequestResponse {

    @Schema(description = "The internal ID of the deletion request")
    private int id;

    @Schema(description = "When the deletion request was created")
    private Date createdAt;

    @Schema(description = "When the request will be automatically approved if not acted on")
    private Date autoApproveBy;

    @Schema(description = "Whether the request was auto-approved by the scheduler")
    private boolean autoApproved;

    @Schema(description = "Admin reason for delaying, null if not delayed")
    private String delayNote;

    @Schema(description = "The agency this deletion request is for")
    private AgencyResponse agency;

    @Schema(description = "The user who submitted the deletion request")
    private UserResponse requestedBy;

    @Schema(description = "The admin who last reviewed (delayed) this request, null if unreviewed")
    private UserResponse reviewedBy;
}
