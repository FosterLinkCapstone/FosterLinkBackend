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
@Schema(description = "Paginated list of approved FAQs with total page count.")
public class GetFaqsResponse implements Serializable {

    @Schema(description = "The FAQs for the requested page.")
    private List<FaqResponse> faqs;

    @Schema(description = "Total number of pages (ceiling of total approved FAQs / page size).")
    private int totalPages;
}
