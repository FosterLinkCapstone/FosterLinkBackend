package net.fosterlink.fosterlinkbackend.models.web;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLoginModelEmail {

    private String email;
    private String password;

}
