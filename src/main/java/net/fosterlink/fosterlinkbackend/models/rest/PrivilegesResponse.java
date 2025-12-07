package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "The privileges of a user",
        requiredProperties = {"isAdmin", "isFaqAuthor", "isAgent"})
public class PrivilegesResponse {

    @Schema(description = "Whether the user is an administrator")
    private boolean isAdmin;
    @Schema(description = "Whether the user is an FAQ author")
    private boolean isFaqAuthor;
    @Schema(description = "Whether the user is a verified agency representative")
    private boolean isAgent;

}
