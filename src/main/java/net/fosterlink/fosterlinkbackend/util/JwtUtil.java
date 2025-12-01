package net.fosterlink.fosterlinkbackend.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtUtil {

    public static String getLoggedInEmail() {
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() != "anonymousUser") {
            return ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        } else return null;
    }
    public static boolean isLoggedIn() {
        var sch = SecurityContextHolder.getContext().getAuthentication();
        return sch != null && sch.isAuthenticated() && sch.getPrincipal() != "anonymousUser";
    }
}
