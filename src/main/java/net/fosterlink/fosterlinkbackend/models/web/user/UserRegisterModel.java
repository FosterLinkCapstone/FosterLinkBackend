package net.fosterlink.fosterlinkbackend.models.web.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@Schema(description = "The data required to register a new user.", requiredProperties = {"username", "firstName", "lastName", "email", "password"})
public class UserRegisterModel {

    @Schema(description = "Username of the user.", example = "jblair")
    private final String username;
    @Schema(description = "First name of the user.", example = "Jacob")
    private final String firstName;
    @Schema(description = "Last name of the user.", example = "Blair")
    private final String lastName;
    @Schema(description = "Email of the user.", example = "jacob@fosterlink.net")
    private String email;
    @Schema(description = "Phone number of the user. Optional.", example = "123-456-7890")
    private String phoneNumber;
    // true if used email, false if used phone number
    @Schema(description = "Whether the user registered using an email or a phone number. [UNIMPLEMENTED]", defaultValue = "true")
    private boolean registeredUsingEmail = true;
    @Schema(description = "The password of the user.", example = "P@22w0rd")
    private String password;

}
