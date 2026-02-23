package net.fosterlink.fosterlinkbackend.models.web.faq;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicates who hid an FAQ: by an administrator or by the FAQ author (user).")
public enum HiddenFaqType {

    @Schema(description = "FAQ was hidden by an administrator")
    ADMIN,
    @Schema(description = "FAQ was hidden (soft-deleted) by the user who created it")
    USER
}
