package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated list of threads with total page count.")
public class GetThreadsResponse implements Serializable {

    @Schema(description = "The threads for the requested page.")
    private List<ThreadResponse> threads;

    @Schema(description = "Total number of pages (same for all pages; ceiling of total visible threads / page size).")
    private int totalPages;
}
