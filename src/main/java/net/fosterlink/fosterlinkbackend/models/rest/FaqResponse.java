package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@NoArgsConstructor
public class FaqResponse {

    private int id;
    private String title;
    private String summary;
    private Date createdAt;
    private Date updatedAt;
    private UserResponse author;
    private boolean approved;
    private String approvedByUsername;


}
