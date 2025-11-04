package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name="thread_reply")
public class ThreadReplyEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String content;
    private Date createdAt;
    @Nullable
    private Date updatedAt;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="metadata")
    private PostMetadataEntity metadata;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="posted_by")
    private UserEntity postedBy;
}
