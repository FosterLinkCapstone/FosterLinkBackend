package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "An email notification type and whether the current user has disabled it.")
public class EmailPreferenceResponse {

    @Schema(description = "The internal name key of the email notification type (e.g. ROLE_ASSIGNED)")
    private String name;

    @Schema(description = "The display name of the email notification type")
    private String uiName;

    @Schema(description = "Whether the user has individually disabled this email type")
    private boolean disabled;
}
