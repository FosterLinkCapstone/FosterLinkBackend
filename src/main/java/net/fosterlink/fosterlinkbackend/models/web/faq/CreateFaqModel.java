package net.fosterlink.fosterlinkbackend.models.web.faq;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Data required to create a new FAQ",
        requiredProperties = {"title", "summary", "content"})
public class CreateFaqModel {

    @Schema(description = "The title of the FAQ", example = "How do I become a foster parent?")
    private String title;
    @Schema(description = "A brief summary of the FAQ content", example = "Learn about the requirements and process to become a foster parent")
    private String summary;
    @Schema(description = "The full content of the FAQ", example = "To become a foster parent, you must meet certain requirements...")
    private String content;

}
