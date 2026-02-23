package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity for the thread_like table. Represents a like on a forum thread by a user.
 */
@Entity
@Data
@Table(name = "thread_like")
public class ThreadLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    /** ID of the thread that was liked. */
    private int thread;
    /** User who liked the thread. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user")
    private UserEntity user;

}
