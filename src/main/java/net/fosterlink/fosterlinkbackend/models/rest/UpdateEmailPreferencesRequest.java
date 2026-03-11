package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request body for bulk-updating email notification preferences.")
public class UpdateEmailPreferencesRequest {

    @Schema(description = "List of preferences to update")
    private List<EmailPreferenceUpdate> preferences;

    @Data
    @Schema(description = "A single email preference update entry.")
    public static class EmailPreferenceUpdate {

        @Schema(description = "Internal email type name (e.g. ROLE_ASSIGNED)")
        private String name;

        @Schema(description = "Whether the user wants to opt out of this email type")
        private boolean disabled;
    }
}
