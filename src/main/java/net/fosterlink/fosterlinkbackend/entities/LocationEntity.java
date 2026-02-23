package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity for the location table. Represents a physical address (e.g. agency address).
 */
@Entity
@Data
@Table(name = "location")
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** First line of street address. */
    private String addrLine1;
    /** Second line of street address (optional). */
    @Nullable
    private String addrLine2;

    /** City. */
    private String city;
    /** State or region. */
    private String state;
    /** Zip or postal code. */
    private int zipCode;

}
