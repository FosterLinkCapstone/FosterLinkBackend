package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "Data needed to update a reply.", requiredProperties = {"replyId", "content"})
@Data
@AllArgsConstructor
public class UpdateReplyModel {
    // REQUIRED
    @Schema(description = "The internal ID of the reply")
    @Positive
    private int replyId;

    @Schema(description = "The updated content of the reply")
    @NotBlank
    @Size(min=1,max=5000)
    private String content;

}

