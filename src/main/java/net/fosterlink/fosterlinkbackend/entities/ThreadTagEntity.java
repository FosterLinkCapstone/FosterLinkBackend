package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity for the thread_tag table. Represents a tag attached to a forum thread.
 */
@Entity
@Data
@Table(name = "thread_tag")
public class ThreadTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** Tag name (e.g. "advice", "guide"). */
    private String name;

    /** The thread this tag belongs to. */
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "thread")
    private ThreadEntity thread;

}
