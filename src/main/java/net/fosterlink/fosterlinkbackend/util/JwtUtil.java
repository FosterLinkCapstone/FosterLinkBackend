package net.fosterlink.fosterlinkbackend.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtUtil {

    public static String getLoggedInEmail() {
        return ((UserDetails) SecurityContextHolder.getContext().getAuthentication()).getUsername();
    }
    public static boolean isLoggedIn() {
        var sch = SecurityContextHolder.getContext().getAuthentication();
        return sch != null && sch.isAuthenticated();
    }
}
