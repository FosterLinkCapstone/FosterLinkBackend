package net.fosterlink.fosterlinkbackend.models.web.faq;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Data required to create a new FAQ",
        requiredProperties = {"title", "summary", "content"})
public class CreateFaqModel {

    @Schema(description = "The title of the FAQ", example = "How do I become a foster parent?")
    @NotBlank
    @Size(min=5,max=300)
    private String title;
    @Schema(description = "A brief summary of the FAQ content", example = "Learn about the requirements and process to become a foster parent")
    @NotBlank
    @Size(min=10,max=1000)
    private String summary;
    @Schema(description = "The full content of the FAQ", example = "To become a foster parent, you must meet certain requirements...")
    @NotBlank
    @Size(min=20,max=50000)
    private String content;

}
