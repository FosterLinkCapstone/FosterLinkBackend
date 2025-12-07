package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Data required to reply to a thread",
        requiredProperties = {"content", "threadId"})
public class ReplyToThreadModel {

    @Schema(description = "The content of the reply", example = "This is a helpful response to the thread.")
    private String content;
    @Schema(description = "The internal ID of the thread to reply to")
    private int threadId;

}
