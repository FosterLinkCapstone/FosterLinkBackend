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

    private String token;
    private Date expiresAt;
    private String validForEndpoint;
    private int generatedByUserId;
    private Integer targetUserId;
    private String processId;

}
