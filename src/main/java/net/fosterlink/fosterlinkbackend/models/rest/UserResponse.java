package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;

import java.util.Date;

@Data
@Schema(description = "Details about an individual user."
        , requiredProperties = {"id", "fullName", "username", "profilePictureUrl", "createdAt"})
public class UserResponse
{

    public UserResponse(UserEntity userEntity) {
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
    @Schema(description = "Whether or not the user is verified. Includes foster parent, faq author, or agency rep verification. DOES NOT include email or phone number verification, or administrator status.")
    private boolean verified; // faqAuthor, foster, or agency rep (UI should combine all to one)
    @Schema(description = "The date on which the user registered")
    private Date createdAt;

}
