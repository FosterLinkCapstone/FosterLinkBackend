package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "account_deletion_request")
public class AccountDeletionRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String requestedByEmailHash;
    private Date requestedAt;
    @Nullable
    private Date reviewedAt;
    @Column(name = "auto_approved", columnDefinition = "tinyint")
    private boolean autoApproved;
    private Date autoApproveBy;
    @Column(name = "approved", columnDefinition = "tinyint")
    private boolean approved;
    @Nullable
    private String delayNote;
    private boolean clearAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private UserEntity requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @Nullable
    private UserEntity reviewedBy;

}
