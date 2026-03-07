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

}



