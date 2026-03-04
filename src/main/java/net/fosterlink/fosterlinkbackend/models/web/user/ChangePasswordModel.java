package net.fosterlink.fosterlinkbackend.models.web.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request model for changing the logged-in user's password.")
public class ChangePasswordModel {

    @NotBlank
    @Schema(description = "The user's current password, used to verify identity.")
    private String oldPassword;

    @NotBlank
    @Size(min = 12, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$",
             message = "Password must be 12–128 characters and include uppercase, lowercase, a digit, and a special character (@$!%*?&).")
    @Schema(description = "The new password. Must be 12–128 characters and include at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&).")
    private String newPassword;
}
