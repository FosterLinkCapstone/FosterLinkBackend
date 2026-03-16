package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.entities.AccountDeletionRequestEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.models.rest.AccountDeletionRequestResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PaginatedResponse;
import net.fosterlink.fosterlinkbackend.repositories.AccountDeletionRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AccountDeletionRequestMapper;
import net.fosterlink.fosterlinkbackend.service.AccountDeletionService;
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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountDeletionControllerTest {

    @Mock
    private AccountDeletionService accountDeletionService;

    @Mock
    private AccountDeletionRequestRepository deletionRequestRepository;

    @Mock
    private AccountDeletionRequestMapper deletionRequestMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountDeletionController accountDeletionController;

    private UserEntity testUser;
    private LoggedInUser loggedInUser;
    private AccountDeletionRequestEntity pendingRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1);
        testUser.setEmail("user@example.com");
        testUser.setUsername("testuser");

        loggedInUser = new LoggedInUser(1, "user@example.com", 0, "", Set.of(), true, true, true, true, false);

        pendingRequest = new AccountDeletionRequestEntity();
        pendingRequest.setId(10);
        pendingRequest.setRequestedBy(testUser);
        pendingRequest.setRequestedAt(new Date());
        pendingRequest.setApproved(false);
        pendingRequest.setClearAccount(true);
    }

    @Test
    void testRequestDeletion_LoggedIn_NoExistingRequest_ReturnsOk() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(deletionRequestRepository.findPendingByUserId(1)).thenReturn(Optional.empty());
        when(accountDeletionService.requestDeletion(any(UserEntity.class), eq(true))).thenReturn(pendingRequest);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = accountDeletionController.requestDeletion(true);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(accountDeletionService, times(1)).requestDeletion(testUser, true);
        }
    }

    @Test
    void testRequestDeletion_NotLoggedIn_ReturnsForbidden() {
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(null);

            ResponseEntity<?> response = accountDeletionController.requestDeletion(false);

            assertEquals(403, response.getStatusCode().value());
            verify(accountDeletionService, never()).requestDeletion(any(), any(Boolean.class));
        }
    }

    @Test
    void testRequestDeletion_UserAlreadyHasPendingRequest_ReturnsBadRequest() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(deletionRequestRepository.findPendingByUserId(1)).thenReturn(Optional.of(pendingRequest));

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = accountDeletionController.requestDeletion(false);

            assertEquals(400, response.getStatusCode().value());
            verify(accountDeletionService, never()).requestDeletion(any(), any(Boolean.class));
        }
    }

    @Test
    void testCancelDeletion_LoggedIn_HasPendingRequest_ReturnsOk() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(deletionRequestRepository.findPendingByUserId(1)).thenReturn(Optional.of(pendingRequest));
        doNothing().when(accountDeletionService).cancelDeletion(any(AccountDeletionRequestEntity.class));

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = accountDeletionController.cancelDeletion();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(accountDeletionService, times(1)).cancelDeletion(pendingRequest);
        }
    }

    @Test
    void testCancelDeletion_LoggedIn_NoPendingRequest_ReturnsNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(deletionRequestRepository.findPendingByUserId(1)).thenReturn(Optional.empty());

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = accountDeletionController.cancelDeletion();

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            verify(accountDeletionService, never()).cancelDeletion(any());
        }
    }

    @Test
    void testGetMyRequest_LoggedIn_HasRequest_ReturnsOkWithResponse() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(deletionRequestRepository.findPendingByUserId(1)).thenReturn(Optional.of(pendingRequest));

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = accountDeletionController.getMyRequest();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertInstanceOf(AccountDeletionRequestResponse.class, response.getBody());
        }
    }

    @Test
    void testGetMyRequest_LoggedIn_NoRequest_ReturnsOkWithNull() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(deletionRequestRepository.findPendingByUserId(1)).thenReturn(Optional.empty());

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = accountDeletionController.getMyRequest();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Test
    void testGetDeletionRequests_ReturnsOkWithPaginatedResponse() {
        List<AccountDeletionRequestResponse> requests = Collections.singletonList(new AccountDeletionRequestResponse());
        when(deletionRequestRepository.countPending()).thenReturn(1);
        when(deletionRequestMapper.getAllPendingByRecency(0)).thenReturn(requests);

        ResponseEntity<?> response = accountDeletionController.getDeletionRequests(0, "recency");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(PaginatedResponse.class, response.getBody());
        PaginatedResponse<?> body = (PaginatedResponse<?>) response.getBody();
        assertEquals(requests, body.getItems());
    }

    @Test
    void testApproveDeletion_Admin_RequestExists_ReturnsOk() {
        LoggedInUser adminUser = new LoggedInUser(2, "admin@example.com", 0, "", Set.of("ADMINISTRATOR"), true, true, true, true, false);
        UserEntity admin = new UserEntity();
        admin.setId(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(admin));
        when(deletionRequestRepository.findById(10)).thenReturn(Optional.of(pendingRequest));
        doNothing().when(accountDeletionService).approveDeletion(any(AccountDeletionRequestEntity.class), any(UserEntity.class));

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(adminUser);

            ResponseEntity<?> response = accountDeletionController.approveDeletion(10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(accountDeletionService, times(1)).approveDeletion(pendingRequest, admin);
        }
    }

    @Test
    void testApproveDeletion_RequestNotFound_ReturnsNotFound() {
        LoggedInUser adminUser = new LoggedInUser(2, "admin@example.com", 0, "", Set.of("ADMINISTRATOR"), true, true, true, true, false);
        UserEntity admin = new UserEntity();
        admin.setId(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(admin));
        when(deletionRequestRepository.findById(999)).thenReturn(Optional.empty());

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(adminUser);

            ResponseEntity<?> response = accountDeletionController.approveDeletion(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            verify(accountDeletionService, never()).approveDeletion(any(), any());
        }
    }
}
