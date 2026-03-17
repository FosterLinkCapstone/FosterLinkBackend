package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;

/**
 * Public-safe DTO for a physical address. Omits the internal primary key (id)
 * exposed by the raw LocationEntity JPA entity.
 */
@Data
@NoArgsConstructor
@Schema(description = "Physical address of an agency, without internal database ID.")
public class LocationResponse {

    public LocationResponse(LocationEntity entity) {
        if (entity == null) return;
        this.addrLine1 = entity.getAddrLine1();
        this.addrLine2 = entity.getAddrLine2();
        this.city = entity.getCity();
        this.state = entity.getState();
        this.zipCode = entity.getZipCode();
    }

    @Schema(description = "First line of street address")
    private String addrLine1;

    @Schema(description = "Second line of street address (optional)")
    @Nullable
    private String addrLine2;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State or region")
    private String state;

    @Schema(description = "Zip or postal code")
    private int zipCode;
}
