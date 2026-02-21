package net.fosterlink.fosterlinkbackend.models.web.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.fosterlink.fosterlinkbackend.models.validation.Order1;
import net.fosterlink.fosterlinkbackend.models.validation.Order2;

@Data
@RequiredArgsConstructor
@Schema(description = "The data required to register a new user.", requiredProperties = {"username", "firstName", "lastName", "email", "password"})
public class UserRegisterModel {

    @Schema(description = "Username of the user.", example = "jblair")
    @NotBlank(message = "Please provide a username!", groups = {Order1.class})
    @Size(min=3, max=30)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must be alphanumeric including underscores.")
    private final String username;
    @Schema(description = "First name of the user.", example = "Jacob")
    @NotBlank
    @Size(min=1, max=50)
    private final String firstName;
    @Schema(description = "Last name of the user.", example = "Blair")
    @NotBlank
    @Size(min=1, max=50)
    private final String lastName;
    @Schema(description = "Email of the user.", example = "jacob@fosterlink.net")
    @NotBlank
    @Size(min=1, max=255)
    @Email
    private String email;
    @Schema(description = "Phone number of the user.", example = "123-456-7890")
    @Size(min=1, max=255)
    @NotBlank
    private String phoneNumber;
    @Schema(description = "The password of the user.", example = "P@22w0rd")
    // one number, one uppercase, one lowercase, one special char, 12 chars min (TODO - needs documentation)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$", message = "Password does not meet complexity requirements!")
    @Size(min=12, max=128)
    @NotBlank
    private String password;

}
