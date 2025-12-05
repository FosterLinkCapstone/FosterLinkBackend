package net.fosterlink.fosterlinkbackend.models.web.agency;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class CreateAgencyModel {
    @NotBlank
    @Size(max=255)
    private String name;
    @NotBlank
    private String missionStatement;
    @NotBlank
    @URL
    private String websiteUrl;

    @NotBlank
    private String locationCity;
    @NotBlank
    private String locationState;
    @Min(501)
    @Max(99950)
    private int locationZipCode;
    @NotBlank
    private String locationAddrLine1;
    // nullable
    private String locationAddrLine2;

}
