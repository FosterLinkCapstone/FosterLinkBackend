package net.fosterlink.fosterlinkbackend.models.rest.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.models.rest.PostMetadataResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;

import java.io.Serializable;
import java.util.Date;

/**
 * Admin-only response for a reply owned by a specific user. Includes post metadata
 * so admins can discern hidden, user-deleted, etc., without changing entity models.
 */
@Data
@NoArgsConstructor
@Schema(description = "Reply for a user (admin view), including hidden/visibility state.")
public class AdminReplyForUserResponse implements Serializable {

    @Schema(description = "The internal ID of the reply")
    private int id;
    @Schema(description = "The content of the reply")
    private String content;
    @Schema(description = "When the reply was created")
    private Date createdAt;
    @Schema(description = "When the reply was last updated. Null if never updated.")
    private Date updatedAt;
    @Schema(description = "The ID of the parent thread")
    private int threadId;
    @Schema(description = "Title of the parent thread (for admin 'replying to' display)")
    private String threadTitle;
    @Schema(description = "Username of the parent thread author (for 'Replying to @username' display)")
    private String threadAuthorUsername;
    @Schema(description = "The author of the reply")
    private UserResponse author;
    @Schema(description = "Visibility and moderation metadata (hidden, userDeleted, locked, verified, hiddenBy)")
    private PostMetadataResponse postMetadata;
}
