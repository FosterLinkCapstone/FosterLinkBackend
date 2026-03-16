package net.fosterlink.fosterlinkbackend.models.web.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.fosterlink.fosterlinkbackend.models.validation.ValidPassword;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
@Schema(description = "The data required to update a user.")
public class UpdateUserModel {
    // OPTIONAL
    @Size(min=1, max=50)
    @Nullable
    @Schema(description = "The updated first name. Can be null.")
    private String firstName;
    @Schema(description = "The updated last name. Can be null.")
    @Nullable
    @Size(min=1, max=50)
    private String lastName;
    @Schema(description = "The updated email. Can be null.")
    @Nullable
    @Email
    @Size(min=1, max=255)
    private String email;
    @Schema(description = "The updated phone number. Can be null.")
    @Nullable
    @Size(min=1, max=255)
    private String phoneNumber;
    @Schema(description = "The updated password. Can be null.")
    @Nullable
    @ValidPassword
    private String password;
    @Schema(description = "The updated username. Can be null.")
    @Nullable
    @Size(min=3, max=30)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
    @Schema(description = "The updated profile picture URL. Can be null.")
    @Nullable
    @URL
    @Size(max=2048)
    private String profilePictureUrl;

}
