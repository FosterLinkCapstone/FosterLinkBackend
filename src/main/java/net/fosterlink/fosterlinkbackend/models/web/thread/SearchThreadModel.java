package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@Schema(description = "Data required to search for threads",
        requiredProperties = {"searchBy", "searchTerm"})
public class SearchThreadModel {

    @Schema(description = "The search criteria type: TAGS, USERNAME, THREAD_CONTENT, or THREAD_TITLE")
    private SearchBy searchBy;
    @Schema(description = "The search term to match against", example = "foster care")
    private String searchTerm;

}
