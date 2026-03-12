package net.fosterlink.fosterlinkbackend.models.rest.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.models.rest.FaqResponse;

/**
 * Admin-only response for an FAQ answer (authored by a user). Wraps FAQ data with
 * explicit visibility/approval state so admins can discern pending, approved, denied, hidden.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "FAQ answer for a user (admin view), with explicit status (PENDING, APPROVED, DENIED, HIDDEN).")
public class AdminFaqForUserResponse extends AdminEntityForUserResponse<FaqResponse> {

    @Schema(description = "Whether the FAQ was hidden by the author (vs by admin)")
    private boolean hiddenByAuthor;
}
