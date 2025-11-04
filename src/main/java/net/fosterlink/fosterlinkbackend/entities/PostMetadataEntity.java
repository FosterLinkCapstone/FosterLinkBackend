package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="post_metadata")
public class PostMetadataEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private boolean hidden;
    private boolean user_deleted;
    private boolean locked;
    private boolean verified;

}
