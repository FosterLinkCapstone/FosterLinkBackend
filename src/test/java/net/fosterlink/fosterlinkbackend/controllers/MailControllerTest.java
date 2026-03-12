package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.entities.DontSendEmailEntity;
import net.fosterlink.fosterlinkbackend.entities.EmailTypeEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.models.rest.EmailPreferenceResponse;
import net.fosterlink.fosterlinkbackend.models.rest.EmailPreferencesResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UpdateEmailPreferencesRequest;
import net.fosterlink.fosterlinkbackend.repositories.DontSendEmailRepository;
import net.fosterlink.fosterlinkbackend.repositories.EmailTypeRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailTypeRepository emailTypeRepository;

    @Mock
    private DontSendEmailRepository dontSendEmailRepository;

    @InjectMocks
    private MailController mailController;

    private UserEntity testUser;
    private LoggedInUser loggedInUser;
    private EmailTypeEntity emailType;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1);
        testUser.setEmail("user@example.com");
        testUser.setUnsubscribeAll(false);

        loggedInUser = new LoggedInUser(1, "user@example.com", 0, "", Set.of(), true, true, true, true, false);

        emailType = new EmailTypeEntity();
        emailType.setId(1);
        emailType.setName("TEST_TYPE");
        emailType.setUiName("Test Type");
        emailType.setCanDisable(true);
    }

    @Test
    void testGetEmailPreferences_LoggedIn_ReturnsOkWithPreferences() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(emailTypeRepository.findByCanDisableTrue()).thenReturn(List.of(emailType));
        when(dontSendEmailRepository.findAllByUserId(1)).thenReturn(Collections.emptyList());

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = mailController.getEmailPreferences();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(EmailPreferencesResponse.class, response.getBody());
            EmailPreferencesResponse body = (EmailPreferencesResponse) response.getBody();
            assertFalse(body.isUnsubscribedAll());
            assertEquals(1, body.getPreferences().size());
            assertEquals("TEST_TYPE", body.getPreferences().get(0).getName());
        }
    }

    @Test
    void testGetEmailPreferences_UserUnsubscribedAll_ReturnsOkWithAllDisabledFalse() {
        testUser.setUnsubscribeAll(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(emailTypeRepository.findByCanDisableTrue()).thenReturn(List.of(emailType));

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = mailController.getEmailPreferences();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            EmailPreferencesResponse body = (EmailPreferencesResponse) response.getBody();
            assertTrue(body.isUnsubscribedAll());
            assertFalse(body.getPreferences().get(0).isDisabled());
        }
    }

    @Test
    void testGetEmailPreferences_NotLoggedIn_ReturnsNotFound() {
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(null);

            ResponseEntity<?> response = mailController.getEmailPreferences();

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            verify(userRepository, never()).findById(anyInt());
        }
    }

    @Test
    void testUpdateEmailPreferences_LoggedIn_ReturnsOk() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(emailTypeRepository.findByCanDisableTrue()).thenReturn(List.of(emailType));
        when(dontSendEmailRepository.existsByUserIdAndEmailTypeId(1, 1)).thenReturn(false);

        UpdateEmailPreferencesRequest request = new UpdateEmailPreferencesRequest();
        UpdateEmailPreferencesRequest.EmailPreferenceUpdate update = new UpdateEmailPreferencesRequest.EmailPreferenceUpdate();
        update.setName("TEST_TYPE");
        update.setDisabled(true);
        request.setPreferences(List.of(update));

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = mailController.updateEmailPreferences(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(dontSendEmailRepository, times(1)).save(any(DontSendEmailEntity.class));
        }
    }

    @Test
    void testUpdateEmailPreferences_UserUnsubscribedAll_ReturnsConflict() {
        testUser.setUnsubscribeAll(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        UpdateEmailPreferencesRequest request = new UpdateEmailPreferencesRequest();
        request.setPreferences(List.of());

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = mailController.updateEmailPreferences(request);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            verify(dontSendEmailRepository, never()).save(any());
        }
    }

    @Test
    void testUnsubscribeAll_LoggedIn_ReturnsOk() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        doNothing().when(dontSendEmailRepository).deleteAllByUserId(1);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = mailController.unsubscribeAll();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(userRepository, times(1)).save(testUser);
            verify(dontSendEmailRepository, times(1)).deleteAllByUserId(1);
        }
    }

    @Test
    void testResubscribe_LoggedIn_ReturnsOk() {
        testUser.setUnsubscribeAll(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);

            ResponseEntity<?> response = mailController.resubscribe();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(userRepository, times(1)).save(testUser);
            assertFalse(testUser.isUnsubscribeAll());
        }
    }
}
