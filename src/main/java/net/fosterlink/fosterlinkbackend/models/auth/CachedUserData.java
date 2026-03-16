package net.fosterlink.fosterlinkbackend.models.auth;

import java.util.Set;

/**
 * Lightweight projection cached in the "userDetails" Caffeine cache.
 *
 * Only the fields required by JwtAuthFilter and the restriction interceptor are stored here.
 * The BCrypt password hash is intentionally excluded so that a heap dump or actuator/heapdump
 * cannot expose hashed credentials for cached users (GAP-06 / 05/F-07).
 *
 * The BCrypt hash is still loaded (un-cached) by UserService.loadUserByUsername() which is
 * called only during the login flow by Spring's AuthenticationManager.
 */
public record CachedUserData(
        int databaseId,
        String email,
        int authTokenVersion,
        Set<String> roles,
        boolean restricted
) {}
