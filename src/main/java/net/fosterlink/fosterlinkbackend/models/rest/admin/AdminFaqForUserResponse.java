package net.fosterlink.fosterlinkbackend.models.rest.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.models.rest.FaqResponse;

import java.io.Serializable;

/**
 * Admin-only response for an FAQ answer (authored by a user). Wraps FAQ data with
 * explicit visibility/approval state so admins can discern pending, approved, denied, hidden.
 * Does not change existing FaqResponse.
 */
@Data
@NoArgsConstructor
@Schema(description = "FAQ answer for a user (admin view), with explicit status (PENDING, APPROVED, DENIED, HIDDEN).")
public class AdminFaqForUserResponse implements Serializable {

    @Schema(description = "Full FAQ details (same as public FaqResponse)")
    private FaqResponse faq;

    @Schema(description = "Explicit status: PENDING, APPROVED, DENIED, or HIDDEN")
    private String entityStatus;

    @Schema(description = "Whether the FAQ is currently hidden")
    private boolean hidden;

    @Schema(description = "Whether the FAQ was hidden by the author (vs by admin)")
    private boolean hiddenByAuthor;
}
