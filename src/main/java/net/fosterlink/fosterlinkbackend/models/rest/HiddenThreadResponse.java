package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "A thread that has been hidden, including full post metadata.")
public class HiddenThreadResponse extends ThreadResponse {

    @Schema(description = "Full post metadata for this thread (visibility, locked, verified, hiddenBy, etc.).")
    private PostMetadataResponse postMetadata;

}
