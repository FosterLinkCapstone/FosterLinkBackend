package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * JPA entity for the thread_reply table. Represents a reply (comment) to a forum thread,
 * with content, metadata (visibility), and author.
 */
@Entity
@Data
@Table(name = "thread_reply")
public class ThreadReplyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    /** Reply body content. */
    private String content;
    /** When the reply was created. */
    private Date createdAt;
    /** When the reply was last updated. Null if never updated. */
    @Nullable
    private Date updatedAt;
    /** ID of the parent thread. */
    private int thread_id;
    /** Visibility and moderation metadata for this reply. */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "metadata")
    private PostMetadataEntity metadata;
    /** User who wrote the reply. */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "posted_by")
    private UserEntity postedBy;
}
