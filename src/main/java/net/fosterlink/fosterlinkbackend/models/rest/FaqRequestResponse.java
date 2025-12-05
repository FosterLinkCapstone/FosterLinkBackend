package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.Data;

@Data
public class FaqRequestResponse {

    private int id;
    private String suggestion;
    private String suggestingUsername;

}
