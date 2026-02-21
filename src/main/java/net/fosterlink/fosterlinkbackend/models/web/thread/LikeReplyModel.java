package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Data required to like or unlike a thread reply",
        requiredProperties = {"replyId"})
public class LikeReplyModel {

    @Schema(description = "The internal ID of the reply to like or unlike")
    @Positive
    private int replyId;

}
