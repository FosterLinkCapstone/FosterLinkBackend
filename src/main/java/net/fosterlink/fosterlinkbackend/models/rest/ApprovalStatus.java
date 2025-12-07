package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The approval status of an FAQ or agency")
public enum ApprovalStatus {

    @Schema(description = "The item has been approved")
    APPROVED,
    @Schema(description = "The item has been denied")
    DENIED,
    @Schema(description = "The item is pending approval")
    PENDING;

    public static ApprovalStatus fromDbVal(int approvalStatus) {
        return switch (approvalStatus) {
            case 1 -> APPROVED;
            case 2 -> DENIED;
            default -> PENDING;
        };
    }

}
