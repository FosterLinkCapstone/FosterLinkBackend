package net.fosterlink.fosterlinkbackend.models.web.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "The data required to update a user.", requiredProperties = {"userId"})
public class UpdateUserModel {
    // REQUIRED
    @Schema(description = "The internal ID of the user to update.")
    private int userId;

    // OPTIONAL
    @Schema(description = "The updated first name. Can be null.")
    private String firstName;
    @Schema(description = "The updated last name. Can be null.")
    private String lastName;
    @Schema(description = "The updated email. Can be null.")
    private String email;
    @Schema(description = "The updated phone number. Can be null.")
    private String phoneNumber;
    @Schema(description = "The updated password. Can be null.")
    private String password;
    @Schema(description = "The updated username. Can be null.")
    private String username;
    @Schema(description = "The updated profile picture URL. Can be null.")
    private String profilePictureUrl;

}
