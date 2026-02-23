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
@Schema(description = "Paginated list of agencies with total page count.")
public class GetAgenciesResponse implements Serializable {

    @Schema(description = "The agencies for the requested page.")
    private List<AgencyResponse> agencies;

    @Schema(description = "Total number of pages (ceiling of total agencies / page size).")
    private int totalPages;
}
