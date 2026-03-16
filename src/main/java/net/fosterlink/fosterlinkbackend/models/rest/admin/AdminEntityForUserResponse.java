package net.fosterlink.fosterlinkbackend.models.rest.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Schema(description = "Admin entity wrapper with explicit status (PENDING, APPROVED, DENIED, HIDDEN).")
public class AdminEntityForUserResponse<T> implements Serializable {

    @Schema(description = "Full entity details")
    private T entity;

    @Schema(description = "Explicit status: PENDING, APPROVED, DENIED, or HIDDEN")
    private String entityStatus;

    @Schema(description = "Whether the entity is currently hidden")
    private boolean hidden;
}
