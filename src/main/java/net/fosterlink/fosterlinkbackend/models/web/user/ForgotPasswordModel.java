package net.fosterlink.fosterlinkbackend.models.web.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request model for initiating a password reset. A reset email will be sent if the address matches a registered account.")
public class ForgotPasswordModel {

    @NotBlank
    @Email
    @Schema(description = "The email address associated with the account.")
    private String email;

}
