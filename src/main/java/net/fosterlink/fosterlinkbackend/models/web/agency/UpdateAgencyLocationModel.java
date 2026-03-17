package net.fosterlink.fosterlinkbackend.models.web.agency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Data required to update an agency's location/address")
public class UpdateAgencyLocationModel {

    @NotNull
    @Schema(description = "The ID of the agency whose location to update", required = true)
    private Integer agencyId;

    @Valid
    @Schema(description = "The agency's address/location", required = true)
    private LocationInput location;
}
