package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.Set;

@Data
@Entity
@Table(name="thread")
public class ThreadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;
    private String content;
    private Date createdAt;
    @Nullable
    private Date updatedAt;


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="metadata")
    private PostMetadataEntity postMetadata;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="posted_by")
    private UserEntity postedBy;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "thread")
    private Set<ThreadTagEntity> tags;

    @OneToMany(fetch=FetchType.LAZY, mappedBy = "thread")
    private Set<ThreadLikeEntity>  likes;

}
