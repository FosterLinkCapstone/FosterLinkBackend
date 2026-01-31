package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadTagEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "Details about an individual thread."
        , requiredProperties = {"id", "title", "content", "createdAt", "author", "likeCount", "commentCount", "userPostCount", "tags"})
@NoArgsConstructor
public class ThreadResponse implements Serializable {

    public ThreadResponse(ThreadEntity threadEntity, int likeCount, int commentCount, int userPostCount) {
        this.id = threadEntity.getId();
        this.title = threadEntity.getTitle();
        this.content = threadEntity.getContent();
        this.createdAt = threadEntity.getCreatedAt();
        this.updatedAt = threadEntity.getUpdatedAt();
        this.author = new UserResponse(threadEntity.getPostedBy());
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.userPostCount = userPostCount;
        // Tags should be set separately to avoid accessing lazy-loaded collection
        // Caller should use setTags() method after fetching tags via batch query
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
    }

    /**
     * Backwards-compatible constructor. Defaults:
     * - commentCount: 0
     * - userPostCount: 0
     *
     * Prefer using {@link #ThreadResponse(ThreadEntity, int, int, int)} so callers can populate the computed fields.
     */
    public ThreadResponse(ThreadEntity threadEntity, int likeCount) {
        this(threadEntity, likeCount, 0, 0);
    }
    @Schema(description = "The internal ID of the thread")
    private int id;
    @Schema(description = "The title of the thread")
    private String title;
    @Schema(description = "The content of the thread")
    private String content;
    @Schema(description = "The date and time that the thread was inserted into the database")
    private Date createdAt;
    @Schema(description = "The date and time that the thread was last updated. Can be null.")
    private Date updatedAt;
    @Schema(description = "The author of the thread")
    private UserResponse author;

    @Schema(description = "The number of likes that the thread had at the time of request")
    private int likeCount;

    @Schema(description = "The number of visible comments/replies on this thread at the time of request")
    private int commentCount;

    @Schema(description = "The number of visible threads posted by this thread's author at the time of request")
    private int userPostCount;

    @Schema(description = "Whether the currently logged-in user has liked this thread. Always false if not logged in.")
    private boolean isLiked;

    @Schema(description = "A list of tags that the thread has")
    private List<String> tags;

}
