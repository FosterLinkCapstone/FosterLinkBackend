package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Data required to reply to a thread",
        requiredProperties = {"content", "threadId"})
public class ReplyToThreadModel {

    @Schema(description = "The content of the reply", example = "This is a helpful response to the thread.")
    @NotBlank
    @Size(max=5000)
    private String content;
    @Schema(description = "The internal ID of the thread to reply to")
    @Positive
    private int threadId;

}
