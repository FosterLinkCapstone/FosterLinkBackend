package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFaqAuthor(false);
        testUser.setEmailVerified(false);
        testUser.setIdVerified(false);
        testUser.setVerifiedFoster(false);
        testUser.setVerifiedAgencyRep(false);
        testUser.setAdministrator(false);
        testUser.setCreatedAt(new Date());
    }

    @Test
    void testLoadUserByUsername_UserExists_ReturnsUserDetails() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Act
        UserDetails result = userService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(result);
        assertInstanceOf(LoggedInUser.class, result);
        assertEquals("test@example.com", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isCredentialsNonExpired());
        assertTrue(result.isAccountNonLocked());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void testLoadUserByUsername_UserNotFound_ReturnsNull() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        // Act
        UserDetails result = userService.loadUserByUsername("nonexistent@example.com");

        // Assert
        assertNull(result);
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void testLoadUserByUsername_UserWithAllAuthorities_ReturnsCorrectAuthorities() {
        // Arrange
        testUser.setFaqAuthor(true);
        testUser.setEmailVerified(true);
        testUser.setIdVerified(true);
        testUser.setVerifiedFoster(true);
        testUser.setVerifiedAgencyRep(true);
        testUser.setAdministrator(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Act
        UserDetails result = userService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(result);
        LoggedInUser loggedInUser = (LoggedInUser) result;
        Collection<? extends GrantedAuthority> authorities = loggedInUser.getAuthorities();
        Set<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        
        assertTrue(authorityStrings.contains("USER"));
        assertTrue(authorityStrings.contains("FAQ_AUTHOR"));
        assertTrue(authorityStrings.contains("EMAIL_VERIFIED"));
        assertTrue(authorityStrings.contains("ID_VERIFIED"));
        assertTrue(authorityStrings.contains("FOSTER_VERIFIED"));
        assertTrue(authorityStrings.contains("AGENCY_REP"));
        assertTrue(authorityStrings.contains("ADMINISTRATOR"));
        assertEquals(7, authorityStrings.size());
    }

    @Test
    void testLoadUserByUsername_UserWithNoSpecialAuthorities_ReturnsOnlyUserAuthority() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Act
        UserDetails result = userService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(result);
        LoggedInUser loggedInUser = (LoggedInUser) result;
        Collection<? extends GrantedAuthority> authorities = loggedInUser.getAuthorities();
        Set<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        
        assertTrue(authorityStrings.contains("USER"));
        assertEquals(1, authorityStrings.size());
    }

    @Test
    void testLoadUserByUsername_UserWithPartialAuthorities_ReturnsCorrectSubset() {
        // Arrange
        testUser.setFaqAuthor(true);
        testUser.setEmailVerified(true);
        testUser.setAdministrator(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Act
        UserDetails result = userService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(result);
        LoggedInUser loggedInUser = (LoggedInUser) result;
        Collection<? extends GrantedAuthority> authorities = loggedInUser.getAuthorities();
        Set<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        
        assertTrue(authorityStrings.contains("USER"));
        assertTrue(authorityStrings.contains("FAQ_AUTHOR"));
        assertTrue(authorityStrings.contains("EMAIL_VERIFIED"));
        assertTrue(authorityStrings.contains("ADMINISTRATOR"));
        assertFalse(authorityStrings.contains("ID_VERIFIED"));
        assertFalse(authorityStrings.contains("FOSTER_VERIFIED"));
        assertFalse(authorityStrings.contains("AGENCY_REP"));
        assertEquals(4, authorityStrings.size());
    }
}

