package net.fosterlink.fosterlinkbackend.models.web.faq;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.fosterlink.fosterlinkbackend.config.validation.MaxNewlines;

@Data
@Schema(description = "Data to update an existing FAQ. At least one of title, summary, or content must be provided. Saving sends the FAQ back to pending approval.")
public class UpdateFaqModel {

    @Schema(description = "The ID of the FAQ to update", required = true)
    private int id;

    @Nullable
    @Size(min = 5, max = 300)
    @Schema(description = "Updated title")
    private String title;

    @Nullable
    @Size(min = 10, max = 1000)
    @MaxNewlines(10)
    @Schema(description = "Updated summary")
    private String summary;

    @Nullable
    @Size(min = 20, max = 50000)
    @MaxNewlines(50)
    @Schema(description = "Updated full content")
    private String content;

}
