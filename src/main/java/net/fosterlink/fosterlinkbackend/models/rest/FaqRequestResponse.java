package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Details about an FAQ suggestion request",
        requiredProperties = {"id", "suggestion", "suggestingUsername"})
public class FaqRequestResponse {

    @Schema(description = "The internal ID of the FAQ request")
    private int id;
    @Schema(description = "The suggested FAQ topic or question")
    private String suggestion;
    @Schema(description = "The username of the user who made the suggestion")
    private String suggestingUsername;

}
