package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Profile summary for a user: thread count, FAQ count, agency info, and privileges.",
        requiredProperties = {"userId", "isAdmin", "isFaqAuthor", "agencyCount", "user"})
public class ProfileMetadataResponse {

    @Schema(description = "The internal ID of the user")
    private int userId;
    @Schema(description = "Whether the user is an administrator")
    private boolean isAdmin;
    @Schema(description = "Whether the user is an FAQ author")
    private boolean isFaqAuthor;
    @Schema(description = "The internal ID of the user's primary agency. Null if the user is not an agency representative.")
    @Nullable
    private String agencyId;
    @Schema(description = "The name of the user's primary agency. Null if the user is not an agency representative.")
    @Nullable
    private String agencyName;
    @Schema(description = "First agency name (for backward compatibility, same as agencyName). Null if not an agency representative.")
    @Nullable
    private String firstAgencyName;
    @Schema(description = "The number of agencies the user represents. 0 if none.")
    private int agencyCount;
    @Schema(description = "Basic user information (id, fullName, username, profilePictureUrl, verified, createdAt)")
    private UserResponse user;

}
