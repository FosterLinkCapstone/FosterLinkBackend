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

    private String email;
    private String phoneNumber;
    private String password;

    private String profilePictureUrl;

    private boolean idVerified;
    private boolean verifiedFoster;
    private boolean verifiedAgencyRep;
    private boolean administrator;
    private boolean faqAuthor;
    private boolean emailVerified;
    private Date createdAt;
    private Date updatedAt;

    public void setPassword(String password) {
        // TODO encode
        this.password = password;
    }


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
    private List<FaqEntity> faqsAuthored;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "postedBy")
    private List<ThreadEntity> threadsAuthored;

    @OneToOne(mappedBy = "user")
    @Nullable
    private AgentEntity agent;

}
