package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated list of agency deletion requests with total page count.")
public class GetAgencyDeletionRequestsResponse {

    @Schema(description = "The deletion requests for the requested page.")
    private List<AgencyDeletionRequestResponse> requests;

    @Schema(description = "Total number of pages.")
    private int totalPages;
}
