package net.fosterlink.fosterlinkbackend.util;

import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private LoggedInUser testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        Set<String> authorities = new HashSet<>();
        authorities.add("USER");
        testUser = new LoggedInUser(1, "test@example.com", "password", authorities, true, true, true, true);
    }

    @Test
    void testGetLoggedInEmail_WhenAuthenticated_ReturnsEmail() {
        // Arrange
        Authentication auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String result = JwtUtil.getLoggedInEmail();

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result);
    }

    @Test
    void testGetLoggedInEmail_WhenNotAuthenticated_ReturnsNull() {
        // Arrange
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(context);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            JwtUtil.getLoggedInEmail();
        });
    }

    @Test
    void testGetLoggedInEmail_WhenAnonymousUser_ReturnsNull() {
        // Arrange
        Authentication auth = new UsernamePasswordAuthenticationToken("anonymousUser", null);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String result = JwtUtil.getLoggedInEmail();

        // Assert
        assertNull(result);
    }

    @Test
    void testIsLoggedIn_WhenAuthenticated_ReturnsTrue() {
        // Arrange
        Authentication auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // Act
        boolean result = JwtUtil.isLoggedIn();

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsLoggedIn_WhenNotAuthenticated_ReturnsFalse() {
        // Arrange
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(context);

        // Act
        boolean result = JwtUtil.isLoggedIn();

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsLoggedIn_WhenAnonymousUser_ReturnsFalse() {
        // Arrange
        Authentication auth = new UsernamePasswordAuthenticationToken("anonymousUser", null);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // Act
        boolean result = JwtUtil.isLoggedIn();

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsLoggedIn_WhenNoSecurityContext_ReturnsFalse() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act
        boolean result = JwtUtil.isLoggedIn();

        // Assert
        assertFalse(result);
    }
}

