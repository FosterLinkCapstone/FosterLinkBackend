package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "Data needed to update a reply.", requiredProperties = {"replyId", "content"})
@Data
@AllArgsConstructor
public class UpdateReplyModel {
    // REQUIRED
    @Schema(description = "The internal ID of the reply")
    private int replyId;

    @Schema(description = "The updated content of the reply")
    private String content;

}

