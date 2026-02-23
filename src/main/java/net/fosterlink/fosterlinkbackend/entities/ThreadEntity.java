package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.Set;

/**
 * JPA entity for the thread table. Represents a forum thread (post) with title, content,
 * metadata (visibility, locked, etc.), author, tags, and likes.
 */
@Data
@Entity
@Table(name="thread")
public class ThreadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** Thread title. */
    private String title;
    /** Thread body content. */
    private String content;
    /** When the thread was created. */
    private Date createdAt;
    /** When the thread was last updated. Null if never updated. */
    @Nullable
    private Date updatedAt;

    /** Visibility and moderation metadata (hidden, locked, verified, etc.). */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="metadata")
    private PostMetadataEntity postMetadata;
    /** User who created the thread. */
    @ManyToOne
    @JoinColumn(name="posted_by")
    private UserEntity postedBy;

    /** Tags attached to this thread. */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "thread")
    private Set<ThreadTagEntity> tags;

    /** Likes on this thread. */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "thread")
    private Set<ThreadLikeEntity> likes;

}
