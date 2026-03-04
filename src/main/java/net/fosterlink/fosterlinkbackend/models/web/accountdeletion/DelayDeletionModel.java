package net.fosterlink.fosterlinkbackend.models.web.accountdeletion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for delaying an account deletion request.")
public class DelayDeletionModel {

    @NotNull
    @Schema(description = "The internal ID of the deletion request to delay", required = true)
    private Integer requestId;

    @NotBlank
    @Size(max = 1500, message = "Delay reason must be 1500 characters or fewer")
    @Schema(description = "The administrator's reason for delaying the deletion", required = true)
    private String reason;
}
