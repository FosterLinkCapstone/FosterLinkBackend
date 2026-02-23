package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * JPA entity for the user table. Represents a registered user account with profile data,
 * authentication fields, and role flags (administrator, FAQ author, verified agency rep, etc.).
 */
@Entity
@Table(name="user")
@Data
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** User's first name. */
    private String firstName;
    /** User's last name. */
    private String lastName;

    /** Unique username. */
    private String username;
    /** Unique email address. */
    private String email;
    /** Phone number. */
    private String phoneNumber;
    /** Encoded password. */
    private String password;

    /** URL of the user's profile picture. */
    private String profilePictureUrl;

    /** Whether the user's ID has been verified. */
    private boolean idVerified = false;
    /** Whether the user is a verified foster parent. */
    private boolean verifiedFoster = false;
    /** Whether the user is a verified agency representative. */
    private boolean verifiedAgencyRep = false;
    /** Whether the user has administrator privileges. */
    private boolean administrator = false;
    /** Whether the user can create and manage FAQs. */
    private boolean faqAuthor = false;
    /** Whether the user's email has been verified. */
    private boolean emailVerified = false;
    /** When the account was created. */
    private Date createdAt;
    /** When the account was last updated. */
    private Date updatedAt;

    /** FAQs authored by this user. */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
    private List<FaqEntity> faqsAuthored;

    /** Forum threads posted by this user. */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "postedBy")
    private List<ThreadEntity> threadsAuthored;

}
