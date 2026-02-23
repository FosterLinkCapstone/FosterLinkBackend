package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Metadata for a post (thread or reply): visibility and moderation state.")
public class PostMetadataResponse implements Serializable {

    @Schema(description = "The internal ID of the post metadata record")
    private int id;

    @Schema(description = "Whether the post is hidden (by admin or user)")
    private boolean hidden;

    @Schema(description = "Whether the post was deleted by the author (user-deleted)")
    private boolean userDeleted;

    @Schema(description = "Whether the post is locked")
    private boolean locked;

    @Schema(description = "Whether the post is verified")
    private boolean verified;

    @Schema(description = "The username of the admin or system that hid this post. Null if hidden by the user themselves.")
    private String hiddenBy;
}
