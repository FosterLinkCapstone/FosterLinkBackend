package net.fosterlink.fosterlinkbackend.mail;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailTypeAnnotationMeta {

    private String name;
    private String uiName;
    private boolean canDisable;

}
