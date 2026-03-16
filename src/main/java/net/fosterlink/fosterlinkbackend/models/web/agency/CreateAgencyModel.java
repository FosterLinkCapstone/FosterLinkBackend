package net.fosterlink.fosterlinkbackend.models.web.agency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Schema(description = "Data required to create a new agency",
        requiredProperties = {"name", "missionStatement", "websiteUrl", "location"})
public class CreateAgencyModel {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "The name of the agency", example = "Hope Foster Care Agency", maxLength = 255)
    private String name;

    @NotBlank
    @Schema(description = "The mission statement of the agency", example = "To provide loving homes for children in need")
    private String missionStatement;

    @NotBlank
    @URL
    @Schema(description = "The website URL of the agency", example = "https://www.hopefostercare.org")
    private String websiteUrl;

    @Valid
    @Schema(description = "The agency's address/location", required = true)
    private LocationInput location;

    @Schema(description = "Whether to show the agent's email and phone number on the public agency page. Defaults to false.")
    private boolean showContactInfo = false;
}
