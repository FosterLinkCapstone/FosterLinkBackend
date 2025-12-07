package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Data required to answer/delete an FAQ suggestion request",
        requiredProperties = {"reqId"})
public class AnswerFaqSuggestionResponse {

    @Schema(description = "The internal ID of the FAQ request to delete")
    private int reqId;

}
