package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Entity
@Table(name="user")
@Data
public class UserEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String firstName;
    private String lastName;

    private String username;
    private String email;
    private String phoneNumber;
    private String password;

    private String profilePictureUrl;

    private boolean idVerified = false;
    private boolean verifiedFoster = false;
    private boolean verifiedAgencyRep = false;
    private boolean administrator = false;
    private boolean faqAuthor = false;
    private boolean emailVerified = false;
    private Date createdAt;
    private Date updatedAt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
    private List<FaqEntity> faqsAuthored;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "postedBy")
    private List<ThreadEntity> threadsAuthored;

}
