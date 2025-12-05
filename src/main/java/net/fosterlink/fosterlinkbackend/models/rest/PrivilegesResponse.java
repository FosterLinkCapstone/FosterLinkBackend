package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrivilegesResponse {

    private boolean isAdmin;
    private boolean isFaqAuthor;
    private boolean isAgent;

}
