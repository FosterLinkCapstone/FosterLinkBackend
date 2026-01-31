package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PrivilegesResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ProfileMetadataResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.models.web.user.UpdateUserModel;
import net.fosterlink.fosterlinkbackend.models.web.user.UserLoginModelEmail;
import net.fosterlink.fosterlinkbackend.models.web.user.UserRegisterModel;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.UserMapper;
import net.fosterlink.fosterlinkbackend.security.JwtTokenProvider;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpSession httpSession;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private UserEntity testUser;
    private UserRegisterModel registerModel;
    private UserLoginModelEmail loginModel;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhoneNumber("1234567890");
        testUser.setCreatedAt(new Date());

        registerModel = new UserRegisterModel("newuser", "New", "User");
        registerModel.setEmail("newuser@example.com");
        registerModel.setPassword("password123");
        registerModel.setPhoneNumber("9876543210");

        loginModel = new UserLoginModelEmail("test@example.com", "password123");
    }

    @Test
    void testRegisterUser_Success_ReturnsToken() {
        // Arrange
        when(userRepository.existsByUsernameOrEmail(anyString(), anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(mock(Authentication.class));
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("testToken");

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // Act
            ResponseEntity<?> response = userController.registerUser(registerModel);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            verify(userRepository, times(1)).save(any(UserEntity.class));
        }
    }

    @Test
    void testRegisterUser_UserAlreadyExists_ReturnsConflict() {
        // Arrange
        when(userRepository.existsByUsernameOrEmail(anyString(), anyString())).thenReturn(true);

        // Act
        ResponseEntity<?> response = userController.registerUser(registerModel);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void testLoginUser_Success_ReturnsToken() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("testToken");

        // Act
        ResponseEntity<?> response = userController.loginUser(loginModel);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testLoginUser_BadCredentials_ReturnsUnauthorized() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act
        ResponseEntity<?> response = userController.loginUser(loginModel);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testLoginUser_Exception_ReturnsNotFound() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Some error"));

        // Act
        ResponseEntity<?> response = userController.loginUser(loginModel);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetUserInfo_UserExists_ReturnsUserResponse() {
        // Arrange
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

            // Act
            ResponseEntity<?> response = userController.getUserInfo();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertInstanceOf(UserResponse.class, response.getBody());
        }
    }

    @Test
    void testGetUserInfo_UserNotFound_ReturnsNotFound() {
        // Arrange
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("nonexistent@example.com");
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

            // Act
            ResponseEntity<?> response = userController.getUserInfo();

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Test
    void testUpdateUser_Success_ReturnsUpdatedUser() {
        // Arrange
        UpdateUserModel updateModel = new UpdateUserModel(1, "Updated", null, null, null, null, null, null);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(true);
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
            when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

            // Act
            ResponseEntity<?> response = userController.updateUser(updateModel, httpServletRequest);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(userRepository, times(1)).save(any(UserEntity.class));
        }
    }

    @Test
    void testUpdateUser_Unauthorized_ReturnsUnauthorized() {
        // Arrange
        UpdateUserModel updateModel = new UpdateUserModel(2, null, null, null, null, null, null, null); // Different user ID

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(true);
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

            // Act
            ResponseEntity<?> response = userController.updateUser(updateModel, httpServletRequest);

            // Assert
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            verify(userRepository, never()).save(any(UserEntity.class));
        }
    }

    @Test
    void testDeleteUser_UserExists_ReturnsOk() {
        // Arrange
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
            doNothing().when(userRepository).delete(any(UserEntity.class));

            // Act
            ResponseEntity<?> response = userController.deleteUser();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(userRepository, times(1)).delete(testUser);
        }
    }

    @Test
    void testDeleteUser_UserNotFound_ReturnsNotFound() {
        // Arrange
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("nonexistent@example.com");
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

            // Act
            ResponseEntity<?> response = userController.deleteUser();

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            verify(userRepository, never()).delete(any(UserEntity.class));
        }
    }

    @Test
    void testGetPrivileges_UserLoggedIn_ReturnsPrivileges() {
        // Arrange
        testUser.setAdministrator(true);
        testUser.setFaqAuthor(true);
        testUser.setVerifiedAgencyRep(true);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(true);
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

            // Act
            ResponseEntity<?> response = userController.getPrivileges();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertInstanceOf(PrivilegesResponse.class, response.getBody());
            PrivilegesResponse privileges = (PrivilegesResponse) response.getBody();
            assertTrue(privileges.isAdmin());
            assertTrue(privileges.isFaqAuthor());
            assertTrue(privileges.isAgent());
        }
    }

    @Test
    void testGetPrivileges_UserNotLoggedIn_ReturnsDefaultPrivileges() {
        // Arrange
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(false);

            // Act
            ResponseEntity<?> response = userController.getPrivileges();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertInstanceOf(PrivilegesResponse.class, response.getBody());
            PrivilegesResponse privileges = (PrivilegesResponse) response.getBody();
            assertFalse(privileges.isAdmin());
            assertFalse(privileges.isFaqAuthor());
            assertFalse(privileges.isAgent());
        }
    }

    @Test
    void testIsUserAdmin_UserIsAdmin_ReturnsTrue() {
        // Arrange
        testUser.setAdministrator(true);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(true);
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

            // Act
            ResponseEntity<?> response = userController.isUserAdmin();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(true, response.getBody());
        }
    }

    @Test
    void testIsUserAdmin_UserNotLoggedIn_ReturnsFalse() {
        // Arrange
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(false);

            // Act
            ResponseEntity<?> response = userController.isUserAdmin();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(false, response.getBody());
        }
    }

    @Test
    void testGetAgentInfo_UserIsAgent_ReturnsAgentInfo() {
        // Arrange
        testUser.setVerifiedAgencyRep(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = userController.getAgentInfo(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(AgentInfoResponse.class, response.getBody());
    }

    @Test
    void testGetAgentInfo_UserNotAgent_ReturnsBadRequest() {
        // Arrange
        testUser.setVerifiedAgencyRep(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = userController.getAgentInfo(1);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetAgentInfo_UserNotFound_ReturnsNotFound() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = userController.getAgentInfo(1);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testLogout_Success_ReturnsOk() throws Exception {
        // Arrange
        when(httpServletRequest.getSession(false)).thenReturn(httpSession);
        doNothing().when(httpSession).invalidate();

        // Act
        ResponseEntity<?> response = userController.logout(httpServletRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(httpSession, times(1)).invalidate();
    }

    @Test
    void testLogout_NoSession_ReturnsOk() {
        // Arrange
        when(httpServletRequest.getSession(false)).thenReturn(null);

        // Act
        ResponseEntity<?> response = userController.logout(httpServletRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetProfileMetadata_UserExists_ReturnsProfileMetadata() {
        // Arrange
        ProfileMetadataResponse metadata = new ProfileMetadataResponse();
        metadata.setUserId(1);
        when(userRepository.existsById(1)).thenReturn(true);
        when(userMapper.mapProfileMetadataResponse(1)).thenReturn(metadata);

        // Act
        ResponseEntity<?> response = userController.getProfileMetadata(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(ProfileMetadataResponse.class, response.getBody());
        assertEquals(1, ((ProfileMetadataResponse) response.getBody()).getUserId());
    }

    @Test
    void testGetProfileMetadata_UserNotFound_ReturnsNotFound() {
        // Arrange
        when(userRepository.existsById(999)).thenReturn(false);

        // Act
        ResponseEntity<?> response = userController.getProfileMetadata(999);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userMapper, never()).mapProfileMetadataResponse(anyInt());
    }

    @Test
    void testGetProfileMetadata_UserExistsButNoProfileData_ReturnsNotFound() {
        // Arrange
        when(userRepository.existsById(1)).thenReturn(true);
        when(userMapper.mapProfileMetadataResponse(1)).thenReturn(null);

        // Act
        ResponseEntity<?> response = userController.getProfileMetadata(1);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

