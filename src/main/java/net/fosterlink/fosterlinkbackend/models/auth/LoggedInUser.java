package net.fosterlink.fosterlinkbackend.models.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Spring Security user details implementation for the currently authenticated user.
 * Wraps database user id, email, encoded password, and role authorities for use in the security context.
 */
@AllArgsConstructor
@Data
public class LoggedInUser implements UserDetails {

    /** Primary key of the user in the database. */
    private int databaseId;
    /** User email (used as username for authentication). */
    private String email;
    /** Encoded password. */
    private String password;
    /** Role names (e.g. ADMINISTRATOR, FAQ_AUTHOR) for authorization. */
    private Set<String> authorities;

    /** Whether the user account is enabled. */
    private boolean enabled;
    /** Whether the account has not expired. */
    private boolean accountNonExpired;
    /** Whether credentials have not expired. */
    private boolean credentialsNonExpired;
    /** Whether the account is not locked. */
    private boolean accountNonLocked;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
