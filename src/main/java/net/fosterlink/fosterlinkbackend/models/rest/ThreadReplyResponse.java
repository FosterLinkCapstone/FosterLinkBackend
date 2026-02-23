package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.entities.ThreadReplyEntity;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "Details about a thread reply",
        requiredProperties = {"id", "content", "createdAt", "author", "likeCount"})
public class ThreadReplyResponse {

    public ThreadReplyResponse(ThreadReplyEntity threadReplyEntity, int likeCount) {
        this.id =  threadReplyEntity.getId();
        this.content = threadReplyEntity.getContent();
        this.createdAt = threadReplyEntity.getCreatedAt();
        this.updatedAt = threadReplyEntity.getUpdatedAt();
        this.author = new UserResponse(threadReplyEntity.getPostedBy());
        this.isLiked = false;
        this.likeCount = likeCount;
    }

    @Schema(description = "The internal ID of the reply")
    private int id;
    @Schema(description = "The content of the reply")
    private String content;
    @Schema(description = "The date and time when the reply was created")
    private Date createdAt;
    @Schema(description = "The date and time when the reply was last updated. Can be null.")
    private Date updatedAt;
    @Schema(description = "Whether the currently logged-in user has liked this reply. Always false if not logged in.")
    private boolean isLiked;
    @Schema(description = "The author of the reply")
    private UserResponse author;
    @Schema(description = "The number of likes this reply has received")
    private int likeCount;
    @Nullable
    @Schema(description = "Post metadata, only populated for admin users viewing hidden replies. Null for regular visible replies.")
    private PostMetadataResponse postMetadata;

}
