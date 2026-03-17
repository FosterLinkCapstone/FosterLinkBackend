package net.fosterlink.fosterlinkbackend.util;

import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
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

    /**
     * Returns the full {@link LoggedInUser} principal from the current SecurityContext,
     * or null if no authenticated user is present.
     */
    public static LoggedInUser getLoggedInUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoggedInUser user) {
            return user;
        }
        return null;
    }

    /**
     * Returns true if the currently authenticated user holds the given authority
     * (e.g. "ADMINISTRATOR", "FAQ_AUTHOR", "AGENCY_REP").
     * Does not trigger a database query — reads directly from the SecurityContext.
     */
    public static boolean hasAuthority(String authority) {
        LoggedInUser user = getLoggedInUser();
        if (user == null) return false;
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
