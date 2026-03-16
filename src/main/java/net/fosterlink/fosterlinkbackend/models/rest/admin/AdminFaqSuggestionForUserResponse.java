package net.fosterlink.fosterlinkbackend.models.rest.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Admin-only response for an FAQ suggestion submitted by a specific user.
 * FAQ suggestions do not have hidden/pending states; this DTO includes createdAt for admin context.
 */
@Data
@NoArgsConstructor
@Schema(description = "FAQ suggestion for a user (admin view).")
public class AdminFaqSuggestionForUserResponse implements Serializable {

    @Schema(description = "The internal ID of the FAQ request/suggestion")
    private int id;
    @Schema(description = "The suggested FAQ topic or question")
    private String suggestion;
    @Schema(description = "The username of the user who made the suggestion")
    private String suggestingUsername;
    @Schema(description = "When the suggestion was submitted")
    private Date createdAt;
}
