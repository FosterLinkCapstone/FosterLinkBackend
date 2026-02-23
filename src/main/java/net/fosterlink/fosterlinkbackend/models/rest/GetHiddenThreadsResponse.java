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
@Schema(description = "Paginated list of hidden threads with total page count.")
public class GetHiddenThreadsResponse implements Serializable {

    @Schema(description = "The hidden threads for the requested page.")
    private List<HiddenThreadResponse> threads;

    @Schema(description = "Total number of pages.")
    private int totalPages;

}
