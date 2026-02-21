package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
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
    @Nullable
    private String hidden_by;

}
