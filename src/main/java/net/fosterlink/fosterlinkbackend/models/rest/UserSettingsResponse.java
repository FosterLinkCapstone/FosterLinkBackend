package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;

@Data
@NoArgsConstructor
@Schema(description = "Editable account settings for the currently logged-in user.")
public class UserSettingsResponse {

    public UserSettingsResponse(UserEntity userEntity) {
        this.id = userEntity.getId();
        this.firstName = userEntity.getFirstName();
        this.lastName = userEntity.getLastName();
        this.email = userEntity.getEmail();
        this.phoneNumber = userEntity.getPhoneNumber();
        this.username = userEntity.getUsername();
        this.profilePictureUrl = userEntity.getProfilePictureUrl();
        this.emailVerified = userEntity.isEmailVerified();
    }

    @Schema(description = "The internal ID of the user")
    private int id;

    @Schema(description = "The user's first name")
    private String firstName;

    @Schema(description = "The user's last name")
    private String lastName;

    @Schema(description = "The user's email address")
    private String email;

    @Schema(description = "The user's phone number")
    private String phoneNumber;

    @Schema(description = "The user's username")
    private String username;

    @Schema(description = "The image URL of the user's profile picture")
    private String profilePictureUrl;

    @Schema(description = "Whether the user's email address has been verified")
    private boolean emailVerified;
}
