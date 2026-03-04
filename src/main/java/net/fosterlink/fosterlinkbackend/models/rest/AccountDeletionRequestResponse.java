package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details about an account deletion request.")
public class AccountDeletionRequestResponse {

    @Schema(description = "The internal ID of the deletion request")
    private int id;

    @Schema(description = "When the deletion request was submitted")
    private Date requestedAt;

    @Schema(description = "The deadline by which this request will be automatically approved")
    private Date autoApproveBy;

    @Schema(description = "When the request was reviewed (approved or delayed) by an administrator. Null if not yet reviewed.")
    @Nullable
    private Date reviewedAt;

    @Schema(description = "Whether the request was automatically approved by the system")
    private boolean autoApproved;

    @Schema(description = "Whether the request has been approved")
    private boolean approved;

    @Schema(description = "The reason provided by the reviewing administrator for delaying the deletion. Null if not delayed.")
    @Nullable
    private String delayNote;

    @Schema(description = "Whether the user requested full account data clearance in addition to account deletion")
    private boolean clearAccount;

    @Schema(description = "The user who submitted the deletion request")
    private UserResponse requestedBy;

    @Schema(description = "The administrator who last reviewed (approved or delayed) this request. Null if not yet reviewed.")
    @Nullable
    private UserResponse reviewedBy;
}
