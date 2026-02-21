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
@Schema(description = "Paginated list of pending FAQs with total page count.")
public class GetPendingFaqsResponse implements Serializable {

    @Schema(description = "The pending FAQs for the requested page.")
    private List<PendingFaqResponse> faqs;

    @Schema(description = "Total number of pages (ceiling of total pending FAQs / page size).")
    private int totalPages;
}
