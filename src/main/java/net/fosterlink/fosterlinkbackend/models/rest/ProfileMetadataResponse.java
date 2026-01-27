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
    private UserResponse user;

}
