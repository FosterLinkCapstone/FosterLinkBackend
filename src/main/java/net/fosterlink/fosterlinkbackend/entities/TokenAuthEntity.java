package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name="token_auth")
public class TokenAuthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** SHA-256 hash of the opaque token value. The plaintext token is never stored. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    private Date expiresAt;
    private String validForEndpoint;
    private int generatedByUserId;
    private Integer targetUserId;
    private String processId;

}
