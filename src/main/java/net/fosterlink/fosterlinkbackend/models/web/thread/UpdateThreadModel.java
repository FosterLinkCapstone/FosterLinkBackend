package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "Data needed to update a thread.", requiredProperties = {"threadId"})
@Data
@AllArgsConstructor
public class UpdateThreadModel {
    // REQUIRED
    @Schema(description = "The internal ID of the thread")
    private int threadId;

    // OPTIONAL
    @Schema(description = "The updated title of the thread. Can be null.")
    private String title;
    @Schema(description = "The updated content of the thread. Can be null.")
    private String content;
    // TODO enable changing tags

}
