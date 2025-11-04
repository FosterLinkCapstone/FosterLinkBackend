package net.fosterlink.fosterlinkbackend.entities;


import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="agent")
public class AgentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="assoc_user")
    @Nullable
    private UserEntity user;

}
