package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadTagEntity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "Details about an individual thread."
        , requiredProperties = {"id", "title", "content", "createdAt", "author", "likeCount", "tags"})
@NoArgsConstructor
public class ThreadResponse implements Serializable {

    public ThreadResponse(ThreadEntity threadEntity, int likeCount) {
        this.id = threadEntity.getId();
        this.title = threadEntity.getTitle();
        this.content = threadEntity.getContent();
        this.createdAt = threadEntity.getCreatedAt();
        this.updatedAt = threadEntity.getUpdatedAt();
        this.author = new UserResponse(threadEntity.getPostedBy());
        this.likeCount = likeCount;
        if (threadEntity.getTags() != null) {
            for (ThreadTagEntity tag : threadEntity.getTags()) {
                tags.add(tag.getName());
            }
        }
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

    private boolean isLiked;

    @Schema(description = "A list of tags that the thread has")
    private List<String> tags;

}
