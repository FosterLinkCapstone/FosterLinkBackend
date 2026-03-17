package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "consent_record")
public class ConsentRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "consent_type", nullable = false, length = 50)
    private String consentType;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Nullable
    @Column(name = "policy_version", length = 20)
    private String policyVersion;

    @Nullable
    @Column(name = "mechanism", length = 50)
    private String mechanism;

    @Nullable
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

}
