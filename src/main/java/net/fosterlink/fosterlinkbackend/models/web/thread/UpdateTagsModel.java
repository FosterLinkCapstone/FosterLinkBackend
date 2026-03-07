package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Request body for updating the tags on an existing thread. Replaces all tags with the provided list.", requiredProperties = {"threadId"})
@Data
public class UpdateTagsModel {

    @Schema(description = "The internal ID of the thread whose tags are being updated.", example = "42", required = true)
    private int threadId;

    @Schema(description = "The new list of tag names. Replaces all existing tags. Empty array removes all tags. Tag names are normalized (trimmed, lowercased).", example = "[\"advice\", \"foster-care\"]")
    private String[] tags;

}
