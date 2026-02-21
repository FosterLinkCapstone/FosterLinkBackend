package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicates who hid a thread: by an administrator or by the thread author (user).")
public enum HiddenThreadType {

    @Schema(description = "Thread was hidden by an administrator")
    ADMIN,
    @Schema(description = "Thread was hidden (soft-deleted) by the user who created it")
    USER
}
