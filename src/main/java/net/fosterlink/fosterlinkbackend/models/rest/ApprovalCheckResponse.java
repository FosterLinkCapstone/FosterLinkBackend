package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Approval status counts for a user's FAQs",
        requiredProperties = {"countPending", "countDenied"})
public class ApprovalCheckResponse {

    @Schema(description = "The number of FAQs that are pending approval")
    private int countPending;
    @Schema(description = "The number of FAQs that have been denied")
    private int countDenied;

}
