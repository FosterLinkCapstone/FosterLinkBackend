package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Data required to create an FAQ suggestion request",
        requiredProperties = {"suggested"})
public class CreateFaqSuggestionModel {

    @Schema(description = "The suggested FAQ topic or question", example = "How do I become a foster parent?")
    private String suggested;

}
