package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Aggregate user counts for the admin dashboard.")
public class AdminUserStatsResponse {

    @Schema(description = "Total number of registered users")
    private long totalUsers;

    @Schema(description = "Total number of administrators")
    private long totalAdministrators;

    @Schema(description = "Total number of FAQ authors")
    private long totalFaqAuthors;

    @Schema(description = "Total number of verified agency reps (agents)")
    private long totalAgents;

    @Schema(description = "Total number of deleted accounts")
    private long totalDeletedAccounts;
}
