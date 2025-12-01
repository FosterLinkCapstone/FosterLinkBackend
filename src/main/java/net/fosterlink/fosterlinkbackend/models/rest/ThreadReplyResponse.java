package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.entities.ThreadReplyEntity;

import java.util.Date;

@Data
@NoArgsConstructor
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

    private int id;
    private String content;
    private Date createdAt;
    private Date updatedAt;
    private boolean isLiked;
    private UserResponse author;
    private int likeCount;

}
