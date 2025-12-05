package net.fosterlink.fosterlinkbackend.models.web.thread;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReplyToThreadModel {

    private String content;
    private int threadId;

}
