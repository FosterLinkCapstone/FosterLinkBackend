package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The search criteria type for thread searches")
public enum SearchBy {

    @Schema(description = "Search by thread tags")
    TAGS,
    @Schema(description = "Search by username of thread author")
    USERNAME,
    @Schema(description = "Search within thread content")
    THREAD_CONTENT,
    @Schema(description = "Search within thread titles")
    THREAD_TITLE

}
