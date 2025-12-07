package net.fosterlink.fosterlinkbackend.models.web.faq;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Data required to approve or deny an FAQ",
        requiredProperties = {"id", "approved"})
public class ApproveFaqModel {

    @Schema(description = "The internal ID of the FAQ to approve or deny")
    private int id;
    @Schema(description = "true to approve the FAQ, false to deny it")
    private boolean approved;

}
