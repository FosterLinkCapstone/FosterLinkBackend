package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * JPA entity for the agency_deletion_request table. Represents a request by an
 * agency owner to have their agency permanently deleted.
 */
@Entity
@Data
@Table(name = "agency_deletion_request")
public class AgencyDeletionRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** false = pending review, true = approved (agency deleted) */
    @Column(name = "approved", columnDefinition = "tinyint")
    private boolean approved;

    @Column(name = "created_at")
    private Date createdAt;

    @Nullable
    private Date reviewedAt;

    @Column(name = "auto_approved", columnDefinition = "tinyint")
    private boolean autoApproved;

    private Date autoApproveBy;

    @Nullable
    private String delayNote;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency")
    private AgencyEntity agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private UserEntity requestedBy;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewedBy;
}
