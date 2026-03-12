package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Details about a pending FAQ",
        requiredProperties = {"id", "title", "summary", "createdAt", "author", "approvalStatus"})
public class PendingFaqResponse extends FaqResponse {

    @Schema(description = "The approval status of the FAQ: APPROVED, DENIED, or PENDING")
    private ApprovalStatus approvalStatus;
    @Schema(description = "The username of the administrator who denied the FAQ. Only present if approvalStatus is DENIED.")
    private String deniedByUsername;
}
