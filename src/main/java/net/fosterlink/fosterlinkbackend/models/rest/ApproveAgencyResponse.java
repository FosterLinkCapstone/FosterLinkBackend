package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Data required to approve or deny an agency",
        requiredProperties = {"id", "approved"})
public class ApproveAgencyResponse {

    @Schema(description = "The internal ID of the agency to approve or deny")
    private int id;
    @Schema(description = "true to approve the agency, false to deny it")
    private boolean approved;

}
