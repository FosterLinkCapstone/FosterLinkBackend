package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity for the thread_reply_like table. Represents a like on a thread reply by a user.
 */
@Entity
@Data
@Table(name = "thread_reply_like")
public class ThreadReplyLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    /** ID of the reply that was liked. */
    private int thread;
    /** User who liked the reply. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user")
    private UserEntity user;

}
