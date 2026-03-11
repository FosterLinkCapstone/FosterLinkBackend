package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Email notification preferences for the current user.")
public class EmailPreferencesResponse {

    @Schema(description = "Whether the user has opted out of all emails entirely")
    private boolean unsubscribedAll;

    @ArraySchema(schema = @Schema(implementation = EmailPreferenceResponse.class))
    private List<EmailPreferenceResponse> preferences;
}
