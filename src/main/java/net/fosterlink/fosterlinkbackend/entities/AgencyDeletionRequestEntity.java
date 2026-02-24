package net.fosterlink.fosterlinkbackend.entities;

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

    /** null = pending review, true = accepted (agency deleted), false = denied */
    private Boolean approved;

    @Column(name = "created_at")
    private Date createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency")
    private AgencyEntity agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private UserEntity requestedBy;
}
