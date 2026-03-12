package net.fosterlink.fosterlinkbackend.controllers;

import jakarta.persistence.EntityManager;
import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.mail.service.AgencyMailService;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ApproveAgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PaginatedResponse;
import net.fosterlink.fosterlinkbackend.repositories.AgencyDeletionRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.repositories.AuditLogRepository;
import net.fosterlink.fosterlinkbackend.repositories.LocationRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AgencyMapper;
import net.fosterlink.fosterlinkbackend.service.AgencyDeletionService;
import net.fosterlink.fosterlinkbackend.service.AuditLogService;
import net.fosterlink.fosterlinkbackend.service.TokenAuthService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgencyControllerTest {

    @Mock
    private AgencyMapper agencyMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AgencyRepository agencyRepository;

    @Mock
    private AgencyDeletionRequestRepository deletionRequestRepository;

    @Mock
    private AgencyDeletionService agencyDeletionService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AgencyMailService agencyMailService;

    @Mock
    private TokenAuthService tokenAuthService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AgencyController agencyController;

    private UserEntity adminUser;
    private UserEntity nonAdminUser;
    private LoggedInUser loggedInAdmin;
    private LoggedInUser loggedInUser;

    @BeforeEach
    void setUp() {
        adminUser = new UserEntity();
        adminUser.setId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setAdministrator(true);

        nonAdminUser = new UserEntity();
        nonAdminUser.setId(2);
        nonAdminUser.setEmail("user@example.com");
        nonAdminUser.setAdministrator(false);

        loggedInAdmin = new LoggedInUser(1, "admin@example.com", 0, "", Set.of("ADMINISTRATOR"), true, true, true, true, false);
        loggedInUser = new LoggedInUser(2, "user@example.com", 0, "", Set.of(), true, true, true, true, false);
    }

    @Test
    void testGetAllAgencies_ReturnsOkWithPaginatedResponse() {
        // Arrange (not logged in: no deletion request info, no current user)
        List<AgencyResponse> agencies = Collections.singletonList(new AgencyResponse());
        when(agencyMapper.getAllApprovedAgencies(eq(0), eq(false), eq(null))).thenReturn(agencies);
        when(agencyMapper.countApprovedWithSearch(isNull(), isNull())).thenReturn(25);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(null);

            // Act
            ResponseEntity<?> response = agencyController.getAllAgencies(0, null, null);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(PaginatedResponse.class, response.getBody());
            PaginatedResponse<?> body = (PaginatedResponse<?>) response.getBody();
            assertEquals(agencies, body.getItems());
            assertEquals(3, body.getTotalPages());
            verify(agencyMapper, times(1)).getAllApprovedAgencies(0, false, null);
        }
    }

    @Test
    void testGetAllAgencies_Admin_IncludesDeletionRequestInfo() {
        List<AgencyResponse> agencies = Collections.singletonList(new AgencyResponse());
        when(agencyMapper.getAllApprovedAgencies(eq(0), eq(true), eq(1))).thenReturn(agencies);
        when(agencyMapper.countApprovedWithSearch(isNull(), isNull())).thenReturn(25);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInAdmin);
            jwtUtilMock.when(() -> JwtUtil.hasAuthority("ADMINISTRATOR")).thenReturn(true);

            ResponseEntity<?> response = agencyController.getAllAgencies(0, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(agencyMapper, times(1)).getAllApprovedAgencies(0, true, 1);
        }
    }

    @Test
    void testGetAllAgencies_Owner_ReceivesCurrentUserIdForOwnAgencies() {
        // Non-admin owner: includeDeletionRequestForAdmin=false, currentUserId so they see deletion request on their own agencies
        List<AgencyResponse> agencies = Collections.singletonList(new AgencyResponse());
        when(agencyMapper.getAllApprovedAgencies(eq(0), eq(false), eq(2))).thenReturn(agencies);
        when(agencyMapper.countApprovedWithSearch(isNull(), isNull())).thenReturn(25);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInUser);
            jwtUtilMock.when(() -> JwtUtil.hasAuthority("ADMINISTRATOR")).thenReturn(false);

            ResponseEntity<?> response = agencyController.getAllAgencies(0, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(agencyMapper, times(1)).getAllApprovedAgencies(0, false, 2);
        }
    }

    @Test
    void testGetPendingAgencies_ReturnsOkWithPaginatedResponse() {
        List<AgencyResponse> pending = Collections.singletonList(new AgencyResponse());
        when(agencyMapper.getAllPendingAgencies(0)).thenReturn(pending);
        when(agencyRepository.countPendingOrDenied()).thenReturn(5);

        ResponseEntity<?> response = agencyController.getPendingAgencies(0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(PaginatedResponse.class, response.getBody());
        PaginatedResponse<?> body = (PaginatedResponse<?>) response.getBody();
        assertEquals(pending, body.getItems());
        assertEquals(1, body.getTotalPages());
        verify(agencyMapper, times(1)).getAllPendingAgencies(0);
    }

    @Test
    void testCountPending_ReturnsOkWithCount() {
        when(agencyRepository.countPending()).thenReturn(5L);

        ResponseEntity<?> response = agencyController.countPending();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody());
        verify(agencyRepository, times(1)).countPending();
    }

    @Test
    void testApproveAgency_Admin_AgencyExists_ReturnsOk() {
        ApproveAgencyResponse model = new ApproveAgencyResponse();
        model.setId(10);
        model.setApproved(true);
        AgencyEntity agency = new AgencyEntity();
        agency.setId(10);
        agency.setAgent(nonAdminUser);

        when(agencyRepository.findById(10)).thenReturn(Optional.of(agency));
        when(agencyRepository.save(any(AgencyEntity.class))).thenReturn(agency);
        when(tokenAuthService.getOrCreateUnsubscribeToken(any(UserEntity.class))).thenReturn("token");

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInAdmin);

            ResponseEntity<?> response = agencyController.approveAgency(model);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(agencyRepository, times(1)).save(any(AgencyEntity.class));
        }
    }

    @Test
    void testApproveAgency_Admin_AgencyNotFound_ReturnsNotFound() {
        ApproveAgencyResponse model = new ApproveAgencyResponse();
        model.setId(999);

        when(agencyRepository.findById(999)).thenReturn(Optional.empty());

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInAdmin);

            ResponseEntity<?> response = agencyController.approveAgency(model);

            assertEquals(404, response.getStatusCode().value());
            verify(agencyRepository, never()).save(any(AgencyEntity.class));
        }
    }
}
