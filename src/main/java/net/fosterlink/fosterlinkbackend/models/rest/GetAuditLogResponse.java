package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Paginated list of audit log entries.")
public class GetAuditLogResponse {

    @Schema(description = "Audit log entries on the current page")
    private List<AuditLogEntryResponse> entries;

    @Schema(description = "Total number of pages")
    private int totalPages;
}
