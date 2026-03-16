package net.fosterlink.fosterlinkbackend.models.web.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.fosterlink.fosterlinkbackend.models.validation.ValidPassword;
import lombok.RequiredArgsConstructor;
import net.fosterlink.fosterlinkbackend.config.validation.Blacklist;
import net.fosterlink.fosterlinkbackend.config.validation.BlacklistMatchBy;
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
    @Blacklist(matchBy = BlacklistMatchBy.STARTS_WITH, value = {"deleted_account", "deleted-account", "deletedaccount", "anonymized_user", "anonymized-user", "anonymizeduser"})
    private final String username;
    @Schema(description = "First name of the user.", example = "Jacob")
    @NotBlank
    @Size(min=1, max=50)
    @Blacklist(value = {"deleted", "account", "deleted_account", "deletedaccount", "deleted-account", "anonymized"})
    private final String firstName;
    @Schema(description = "Last name of the user.", example = "Blair")
    @NotBlank
    @Size(min=1, max=50)
    @Blacklist(value = {"deleted", "account", "deleted_account", "deletedaccount", "deleted-account"})
    @Blacklist(matchBy = BlacklistMatchBy.STARTS_WITH, value = {"user-"})
    private final String lastName;
    @Schema(description = "Email of the user.", example = "jacob@fosterlink.net")
    @NotBlank
    @Size(min=1, max=255)
    @Email
    private String email;
    @Schema(description = "Phone number of the user.", example = "123-456-7890")
    @Size(max=255)
    private String phoneNumber;
    @Schema(description = "The password of the user.", example = "P@22w0rd")
    @ValidPassword
    @NotBlank
    private String password;

    @Schema(description = "Whether the user confirms they are 13 years of age or older. Must be true.", example = "true")
    @AssertTrue(message = "You must confirm you are 13 years of age or older to register.")
    private boolean confirmAgeRequirement;

    @Schema(description = "Whether the user has accepted the Terms of Service. Must be true.", example = "true")
    @AssertTrue(message = "You must accept the Terms of Service to register.")
    private boolean consentTerms;

    @Schema(description = "Whether the user has acknowledged the Privacy Policy. Must be true.", example = "true")
    @AssertTrue(message = "You must acknowledge the Privacy Policy to register.")
    private boolean consentPrivacy;

    @Schema(description = "Whether the user has opted in to marketing emails. Optional, defaults to false.", example = "false")
    private boolean consentMarketing = false;

}
