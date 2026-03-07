package net.fosterlink.fosterlinkbackend.models.web.agency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Data required to update an agency's location/address")
public class UpdateAgencyLocationModel {

    @NotNull
    @Schema(description = "The ID of the agency whose location to update", required = true)
    private Integer agencyId;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "The first line of the agency's street address", example = "123 Main Street", required = true)
    private String locationAddrLine1;

    @Schema(description = "The second line of the agency's street address (optional)", example = "Suite 100")
    private String locationAddrLine2;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "The city where the agency is located", example = "Springfield", required = true)
    private String locationCity;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "The state where the agency is located", example = "IL", required = true)
    private String locationState;

    @NotNull
    @Min(501)
    @Max(99950)
    @Schema(description = "The zip code where the agency is located. Must be between 501 and 99950.", example = "62701", required = true)
    private Integer locationZipCode;
}
