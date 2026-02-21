package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

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
    /** Username of the admin/system that hid the post. Null if hidden by user or not hidden. */
    @Nullable
    private String hidden_by;

}
