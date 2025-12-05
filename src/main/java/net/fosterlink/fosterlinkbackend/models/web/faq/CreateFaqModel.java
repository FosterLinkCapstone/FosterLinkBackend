package net.fosterlink.fosterlinkbackend.models.web.faq;

import lombok.Data;

@Data
public class CreateFaqModel {

    private String title;
    private String summary;
    private String content;

}
