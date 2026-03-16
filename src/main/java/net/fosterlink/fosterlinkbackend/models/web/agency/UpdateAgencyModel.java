package net.fosterlink.fosterlinkbackend.models.web.agency;

import jakarta.annotation.Nullable;
import lombok.Data;

@Data
public class UpdateAgencyModel {

    private int agencyId;

    @Nullable
    private String name;
    @Nullable
    private String missionStatement;
    @Nullable
    private String websiteUrl;

    /** Null means no change. True/false opts in or out of showing contact info publicly. */
    @Nullable
    private Boolean showContactInfo;

}



