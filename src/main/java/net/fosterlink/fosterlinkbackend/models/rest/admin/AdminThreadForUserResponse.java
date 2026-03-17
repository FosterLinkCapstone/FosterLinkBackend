package net.fosterlink.fosterlinkbackend.models.rest.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;

import java.io.Serializable;
import java.util.Date;

/**
 * Admin-only response for a thread owned by a specific user. Includes visibility/moderation
 * state so admins can discern hidden, user-deleted, locked, etc., without changing entity models.
 */
@Data
@NoArgsConstructor
@Schema(description = "Thread for a user (admin view), including hidden/pending state.")
public class AdminThreadForUserResponse implements Serializable {

    @Schema(description = "The internal ID of the thread")
    private int id;
    @Schema(description = "The title of the thread")
    private String title;
    @Schema(description = "The content of the thread")
    private String content;
    @Schema(description = "When the thread was created")
    private Date createdAt;
    @Schema(description = "When the thread was last updated. Null if never updated.")
    private Date updatedAt;
    @Schema(description = "The author of the thread")
    private UserResponse author;

    @Schema(description = "Whether the thread is hidden (by admin or user)")
    private boolean hidden;
    @Schema(description = "Whether the thread was soft-deleted by the author")
    private boolean userDeleted;
    @Schema(description = "Whether the thread is locked")
    private boolean locked;
    @Schema(description = "Whether the thread is verified")
    private boolean verified;
    @Schema(description = "Username of the admin/system that hid the thread. Null if hidden by user or not hidden.")
    private String hiddenBy;
}
