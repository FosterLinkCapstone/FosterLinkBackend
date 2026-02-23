package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "Details about a hidden FAQ")
public class HiddenFaqResponse {

    @Schema(description = "The internal ID of the FAQ")
    private int id;
    @Schema(description = "The title of the FAQ")
    private String title;
    @Schema(description = "A brief summary of the FAQ content")
    private String summary;
    @Schema(description = "The date and time when the FAQ was created")
    private Date createdAt;
    @Schema(description = "The date and time when the FAQ was last updated. Can be null.")
    private Date updatedAt;
    @Schema(description = "The author of the FAQ")
    private UserResponse author;
    @Schema(description = "The username of who hid this FAQ (admin username or author username)")
    private String hiddenBy;
    @Schema(description = "Whether the FAQ was hidden by the author (true) or an administrator (false)")
    private boolean hiddenByAuthor;

}
