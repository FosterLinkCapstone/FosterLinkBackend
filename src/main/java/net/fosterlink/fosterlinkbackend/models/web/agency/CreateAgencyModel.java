package net.fosterlink.fosterlinkbackend.models.web.agency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Schema(description = "Data required to create a new agency",
        requiredProperties = {"name", "missionStatement", "websiteUrl", "locationCity", "locationState", "locationZipCode", "locationAddrLine1"})
public class CreateAgencyModel {
    @NotBlank
    @Size(max=255)
    @Schema(description = "The name of the agency", example = "Hope Foster Care Agency", maxLength = 255)
    private String name;
    @NotBlank
    @Schema(description = "The mission statement of the agency", example = "To provide loving homes for children in need")
    private String missionStatement;
    @NotBlank
    @URL
    @Schema(description = "The website URL of the agency", example = "https://www.hopefostercare.org")
    private String websiteUrl;

    @NotBlank
    @Schema(description = "The city where the agency is located", example = "Springfield")
    private String locationCity;
    @NotBlank
    @Schema(description = "The state where the agency is located", example = "IL")
    private String locationState;
    @Min(501)
    @Max(99950)
    @Schema(description = "The zip code where the agency is located. Must be between 501 and 99950.", example = "62701")
    private int locationZipCode;
    @NotBlank
    @Schema(description = "The first line of the agency's street address", example = "123 Main Street")
    private String locationAddrLine1;
    // nullable
    @Schema(description = "The second line of the agency's street address (optional)", example = "Suite 100")
    private String locationAddrLine2;

}
