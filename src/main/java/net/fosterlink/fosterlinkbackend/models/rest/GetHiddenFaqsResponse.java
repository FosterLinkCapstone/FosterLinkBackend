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
@Schema(description = "Paginated list of hidden FAQs with total page count.")
public class GetHiddenFaqsResponse implements Serializable {

    @Schema(description = "The hidden FAQs for the requested page.")
    private List<HiddenFaqResponse> faqs;

    @Schema(description = "Total number of pages (ceiling of total hidden FAQs / page size).")
    private int totalPages;

}
