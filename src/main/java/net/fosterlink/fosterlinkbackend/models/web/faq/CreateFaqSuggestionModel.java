package net.fosterlink.fosterlinkbackend.models.web.faq;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Data required to create an FAQ suggestion request",
        requiredProperties = {"suggested"})
public class CreateFaqSuggestionModel {

    @Schema(description = "The suggested FAQ topic or question", example = "How do I become a foster parent?")
    @NotBlank
    @Size(min=10,max=2000)
    private String suggested;

}
