package net.fosterlink.fosterlinkbackend.models.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicates who hid an entity: by an administrator or by the author (user).")
public enum HiddenByType {

    @Schema(description = "Entity was hidden by an administrator")
    ADMIN,
    @Schema(description = "Entity was hidden (soft-deleted) by the user who created it")
    USER
}
