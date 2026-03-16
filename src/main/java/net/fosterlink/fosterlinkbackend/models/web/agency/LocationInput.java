package net.fosterlink.fosterlinkbackend.models.web.agency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Address/location input for an agency.")
public class LocationInput {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "The first line of the street address", example = "123 Main Street", required = true)
    private String addrLine1;

    @Schema(description = "The second line of the street address (optional)", example = "Suite 100")
    private String addrLine2;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "The city", example = "Springfield", required = true)
    private String city;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "The state", example = "IL", required = true)
    private String state;

    @NotNull
    @Min(501)
    @Max(99950)
    @Schema(description = "The zip code. Must be between 501 and 99950.", example = "62701", required = true)
    private Integer zipCode;
}
