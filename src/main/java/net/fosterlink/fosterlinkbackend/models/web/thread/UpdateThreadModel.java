package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "Data needed to update a thread.", requiredProperties = {"threadId"})
@Data
@AllArgsConstructor
public class UpdateThreadModel {
    // REQUIRED
    @Schema(description = "The internal ID of the thread")
    @Positive
    private int threadId;

    // OPTIONAL
    @Schema(description = "The updated title of the thread. Can be null.")
    @Nullable
    @Size(min=5, max=200)
    private String title;
    @Schema(description = "The updated content of the thread. Can be null.")
    @Nullable
    @Size(min=5, max=10000)
    private String content;
    // TODO enable changing tags

}
