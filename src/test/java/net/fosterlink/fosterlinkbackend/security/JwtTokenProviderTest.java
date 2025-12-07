package net.fosterlink.fosterlinkbackend.security;

import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "testSecretKeyThatIsAtLeast64CharactersLongForHS512AlgorithmToWorkProperly123456";
    private static final int TEST_EXPIRATION = 3600000; // 1 hour in milliseconds

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExp", TEST_EXPIRATION);
    }

    @Test
    void testGenerateToken_WithValidAuthentication_ReturnsToken() {
        // Arrange
        Set<String> authorities = new HashSet<>();
        authorities.add("USER");
        LoggedInUser user = new LoggedInUser(1, "test@example.com", "password", authorities, true, true, true, true);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        // Act
        String token = jwtTokenProvider.generateToken(auth);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        // JWT tokens typically have 3 parts separated by dots
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void testGetUsernameFromJWT_WithValidToken_ReturnsUsername() {
        // Arrange
        Set<String> authorities = new HashSet<>();
        authorities.add("USER");
        LoggedInUser user = new LoggedInUser(1, "test@example.com", "password", authorities, true, true, true, true);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String token = jwtTokenProvider.generateToken(auth);

        // Act
        String username = jwtTokenProvider.getUsernameFromJWT(token);

        // Assert
        assertNotNull(username);
        assertEquals("test@example.com", username);
    }

    @Test
    void testValidateToken_WithValidToken_ReturnsTrue() {
        // Arrange
        Set<String> authorities = new HashSet<>();
        authorities.add("USER");
        LoggedInUser user = new LoggedInUser(1, "test@example.com", "password", authorities, true, true, true, true);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String token = jwtTokenProvider.generateToken(auth);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_WithInvalidToken_ReturnsFalse() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WithEmptyToken_ReturnsFalse() {
        // Arrange
        String emptyToken = "";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WithNullToken_ReturnsFalse() {
        // Arrange
        String nullToken = null;

        // Act
        boolean isValid = jwtTokenProvider.validateToken(nullToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testGenerateToken_WithDifferentUsers_GeneratesDifferentTokens() {
        // Arrange
        Set<String> authorities1 = new HashSet<>();
        authorities1.add("USER");
        LoggedInUser user1 = new LoggedInUser(1, "user1@example.com", "password", authorities1, true, true, true, true);
        Authentication auth1 = new UsernamePasswordAuthenticationToken(user1, null, user1.getAuthorities());

        Set<String> authorities2 = new HashSet<>();
        authorities2.add("USER");
        LoggedInUser user2 = new LoggedInUser(2, "user2@example.com", "password", authorities2, true, true, true, true);
        Authentication auth2 = new UsernamePasswordAuthenticationToken(user2, null, user2.getAuthorities());

        // Act
        String token1 = jwtTokenProvider.generateToken(auth1);
        String token2 = jwtTokenProvider.generateToken(auth2);

        // Assert
        assertNotEquals(token1, token2);
        assertEquals("user1@example.com", jwtTokenProvider.getUsernameFromJWT(token1));
        assertEquals("user2@example.com", jwtTokenProvider.getUsernameFromJWT(token2));
    }
}

