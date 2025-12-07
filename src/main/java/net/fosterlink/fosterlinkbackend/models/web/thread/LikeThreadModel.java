package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Data required to like or unlike a thread",
        requiredProperties = {"threadId"})
public class LikeThreadModel {

    @Schema(description = "The internal ID of the thread to like or unlike")
    private int threadId;

}
