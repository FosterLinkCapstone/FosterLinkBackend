package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Paginated list of admin user records.")
public class GetAdminUsersResponse {

    @Schema(description = "Users on the current page")
    private List<AdminUserResponse> users;

    @Schema(description = "Total number of pages")
    private int totalPages;
}
