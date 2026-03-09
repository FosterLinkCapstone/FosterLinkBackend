package net.fosterlink.fosterlinkbackend.models.rest.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyResponse;

import java.io.Serializable;

/**
 * Admin-only response for an agency owned by a specific user (agent). Wraps agency data
 * with explicit status fields so admins can discern pending/approved/denied and hidden.
 * Does not change existing AgencyResponse; this adds a clear entityStatus for admin use.
 */
@Data
@NoArgsConstructor
@Schema(description = "Agency for a user (admin view), with explicit status (PENDING, APPROVED, DENIED, HIDDEN).")
public class AdminAgencyForUserResponse implements Serializable {

    @Schema(description = "Full agency details (same as public AgencyResponse)")
    private AgencyResponse agency;

    @Schema(description = "Explicit status: PENDING, APPROVED, DENIED, or HIDDEN")
    private String entityStatus;

    @Schema(description = "Whether the agency is currently hidden")
    private boolean hidden;
}
