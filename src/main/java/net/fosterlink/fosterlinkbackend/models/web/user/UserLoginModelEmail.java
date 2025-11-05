package net.fosterlink.fosterlinkbackend.models.web.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "The data required to login as a user.", requiredProperties = {"email", "password"})
public class UserLoginModelEmail {

    @Schema(description = "The email of the user that is logging in.", example = "jacob@fosterlink.net")
    private String email;
    @Schema(description = "The password of the user that is logging in.", example = "P@22w0rd")
    private String password;

}
