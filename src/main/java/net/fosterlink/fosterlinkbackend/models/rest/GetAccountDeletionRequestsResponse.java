package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated list of account deletion requests with total page count.")
public class GetAccountDeletionRequestsResponse {

    @Schema(description = "The deletion requests for the requested page.")
    private List<AccountDeletionRequestResponse> requests;

    @Schema(description = "Total number of pages")
    private int totalPages;
}
