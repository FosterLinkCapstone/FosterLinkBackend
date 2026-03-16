package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;

import java.util.Date;

/**
 * Public-safe user DTO. Intentionally omits {@code banned} and {@code restricted}
 * fields to prevent enumeration of moderated accounts in public content responses
 * (thread lists, reply lists, agency listings). (04/F-06)
 */
@Data
@NoArgsConstructor
@Schema(description = "Basic public details about a user — safe for inclusion in public content responses.")
public class PublicUserResponse {

    public PublicUserResponse(UserEntity userEntity) {
        this.id = userEntity.getId();
        this.username = userEntity.getUsername();
        this.fullName = userEntity.getFirstName() + " " + userEntity.getLastName();
        this.profilePictureUrl = userEntity.getProfilePictureUrl();
        this.verified = userEntity.isVerifiedFoster() || userEntity.isFaqAuthor() || userEntity.isVerifiedAgencyRep();
        this.createdAt = userEntity.getCreatedAt();
    }

    @Schema(description = "The internal ID of the user")
    private int id;

    @Schema(description = "The combined first and last names of the user")
    private String fullName;

    @Schema(description = "The username of the user")
    private String username;

    @Schema(description = "The image URL of the user's profile picture")
    private String profilePictureUrl;

    @Schema(description = "Whether the user holds a verified status (foster parent, FAQ author, or agency rep). "
            + "Does not reflect email/phone verification or administrator status.")
    private boolean verified;

    @Schema(description = "The date on which the user registered")
    private Date createdAt;
}
