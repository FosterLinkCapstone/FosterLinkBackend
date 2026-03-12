package net.fosterlink.fosterlinkbackend.models.web.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import net.fosterlink.fosterlinkbackend.models.validation.ValidPassword;

@Data
@Schema(description = "Request model for changing the logged-in user's password.")
public class ChangePasswordModel {

    @NotBlank
    @Schema(description = "The user's current password, used to verify identity.")
    private String oldPassword;

    @NotBlank
    @ValidPassword
    @Schema(description = "The new password. Must be 12–128 characters and include at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&).")
    private String newPassword;
}
