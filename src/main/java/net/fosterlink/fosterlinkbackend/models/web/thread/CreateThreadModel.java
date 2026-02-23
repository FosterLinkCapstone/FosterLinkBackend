package net.fosterlink.fosterlinkbackend.models.web.thread;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Schema(description = "Data needed to create a thread", requiredProperties = {"title", "content", "tags"})
@Data
@AllArgsConstructor
public class CreateThreadModel {

    @Schema(description = "The title of the thread.", example = "How to raise child")
    @NotBlank
    @Size(min=5,max=200)
    private String title;
    @Schema(description = "The content of the thread.", example="Lorem ipsum dolor....")
    @NotBlank
    @Size(min=10,max=10000)
    private String content;
    @Schema(description = "The names of the tags attached to the thread. Can be empty.", example = "[\"advice\", \"guide\"]")
    @Size(max = 10)
    @NotNull
    private List<@NotBlank @Size(max=50) String> tags;

}
