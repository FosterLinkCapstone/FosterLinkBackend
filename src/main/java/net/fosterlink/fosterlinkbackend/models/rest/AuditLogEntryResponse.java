package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Single audit log entry with acting and target user display names.")
public class AuditLogEntryResponse {

    @Schema(description = "Database id of the audit log entry")
    private int id;

    @Schema(description = "When the action was recorded")
    private Date createdAt;

    @Schema(description = "Display action (e.g. \"banned user\") — fits in: \"{acting user} {action} {target user}\"")
    private String action;

    @Schema(description = "Target user display: \"{firstname} {lastname} ({username})\"")
    private String targetUser;

    @Schema(description = "Acting user display: \"{firstname} {lastname} ({username})\"")
    private String actingUser;

    @Schema(description = "Full display message: \"{acting user} {action} {target user}\", e.g. \"John Doe (jdoe) banned user George Doe (gdoe)\"")
    private String displayMessage;
}
