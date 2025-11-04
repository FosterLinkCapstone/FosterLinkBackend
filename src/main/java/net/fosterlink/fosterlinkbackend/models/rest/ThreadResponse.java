package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.Data;
import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadTagEntity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ThreadResponse implements Serializable {

    public ThreadResponse(ThreadEntity threadEntity, int likeCount) {
        this.id = threadEntity.getId();
        this.title = threadEntity.getTitle();
        this.content = threadEntity.getContent();
        this.createdAt = threadEntity.getCreatedAt();
        this.updatedAt = threadEntity.getUpdatedAt();
        this.postedByUsername = threadEntity.getPostedBy().getUsername();
        this.postedById = threadEntity.getPostedBy().getId();
        this.likeCount = likeCount;
        for (ThreadTagEntity tag : threadEntity.getTags()) {
            tags.add(tag.getName());
        }
    }

    private int id;
    private String title;
    private String content;
    private Date createdAt;
    private Date updatedAt;

    private String postedByUsername;
    // TODO lazy check username integrity on frontend?
    private int postedById;

    private int likeCount;

    private List<String> tags;

}
