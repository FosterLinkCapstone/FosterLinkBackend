package net.fosterlink.fosterlinkbackend.models.rest.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated list of threads for a user (admin view).")
public class GetAdminThreadsForUserResponse implements Serializable {

    @Schema(description = "Threads on the current page")
    private List<AdminThreadForUserResponse> threads;

    @Schema(description = "Total number of pages")
    private int totalPages;
}
