package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity for the agency table. Represents a foster care agency with contact/location
 * and an associated verified agent (user). Approval state is managed by administrators.
 */
@Entity
@Data
@Table(name="agency")
public class AgencyEntity {

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

    /** Physical address of the agency. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="address")
    private LocationEntity address;
    /** User who represents this agency (verified agency rep). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="agent", nullable = false)
    private UserEntity agent;

}
