package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * JPA entity for the post_metadata table. Holds visibility and moderation state for a
 * thread or reply (hidden, user-deleted, locked, verified, and who hid it).
 */
@Entity
@Data
@Table(name="post_metadata")
public class PostMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** Whether the post is hidden (by admin or user). */
    private boolean hidden;
    /** Whether the post was soft-deleted by the author. */
    private boolean user_deleted;
    /** Whether the post is locked from further replies. */
    private boolean locked;
    /** Whether the post is verified. */
    private boolean verified;
    /** ID of the admin/system user that hid the post. Null if hidden by the author or not hidden. */
    @Column(name = "hidden_by_user_id")
    @Nullable
    private Integer hiddenByUserId;
    /**
     * Timestamp when user_deleted was set to true. Null if the post has never been user-deleted.
     * PostCleanupScheduler hard-deletes rows where user_deleted=true AND deleted_at is 90+ days ago.
     */
    @Nullable
    @Column(name = "deleted_at")
    private Date deletedAt;

}
