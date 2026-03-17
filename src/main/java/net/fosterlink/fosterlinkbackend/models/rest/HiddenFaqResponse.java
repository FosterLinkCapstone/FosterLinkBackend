package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Details about a hidden FAQ")
public class HiddenFaqResponse extends FaqResponse {

    @Schema(description = "The username of who hid this FAQ (admin username or author username)")
    private String hiddenBy;
    @Schema(description = "Whether the FAQ was hidden by the author (true) or an administrator (false)")
    private boolean hiddenByAuthor;
}
