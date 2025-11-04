package net.fosterlink.fosterlinkbackend.models.web;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UserRegisterModel {

    private final String username;
    private final String firstName;
    private final String lastName;
    private String email;
    private String phoneNumber;
    // true if used email, false if used phone number
    private boolean registeredUsingEmail;
    private String password;

}
