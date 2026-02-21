package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@Schema(description = "Data required to search for threads",
        requiredProperties = {"searchBy", "searchTerm"})
public class SearchThreadModel {

    @Schema(description = "The search criteria type: TAGS, USERNAME, THREAD_CONTENT, or THREAD_TITLE")
    @NotNull
    private SearchBy searchBy;
    @Schema(description = "The search term to match against", example = "foster care")
    @NotBlank
    @Size(min=1,max=200)
    private String searchTerm;
    @Schema(description = "The page number to fetch. Pages are of size 10")
    @PositiveOrZero
    private int pageNumber;

}
