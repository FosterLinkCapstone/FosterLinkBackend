package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="thread_reply_like")
public class ThreadReplyLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int thread;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user")
    private UserEntity user;

}
