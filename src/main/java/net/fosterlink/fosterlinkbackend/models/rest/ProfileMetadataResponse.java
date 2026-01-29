package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileMetadataResponse {

    private int userId;
    private boolean isAdmin;
    private boolean isFaqAuthor;
    // nullable
    @Nullable
    private String agencyId;
    // nullable
    @Nullable
    private String agencyName;
    // nullable - first agency name (for backward compatibility, same as agencyName)
    @Nullable
    private String firstAgencyName;
    // number of agencies the user is an agent of (0 if none)
    private int agencyCount;
    private UserResponse user;

}
