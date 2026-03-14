package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * JPA entity for the agency table. Represents a foster care agency with contact/location
 * and an associated verified agent (user). Approval state is managed by administrators.
 */
@Entity
@Data
@Table(name="agency")
public class  AgencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    /** Agency display name. */
    private String name;
    /** Mission statement. */
    private String missionStatement;
    /** Agency website URL. */
    private String websiteUrl;
    /** Approval state: true = approved, false = denied, null = pending. */
    private Boolean approved;
    /** ID of the user (administrator) who approved or denied. */
    private Integer approved_by_id;
    private boolean hidden;

    @Column(name = "hidden_by_user_id")
    @Nullable
    private Integer hiddenByUserId;

    @Column(name = "hidden_by_deletion_request", columnDefinition = "tinyint")
    private boolean hiddenByDeletionRequest;

    @Column(name = "created_at", nullable = false)
    private Date createdAt;
    @Column(name = "updated_at")
    private Date updatedAt;

    /** Physical address of the agency. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="address")
    private LocationEntity address;
    /** User who represents this agency (verified agency rep). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="agent", nullable = false)
    private UserEntity agent;

}
