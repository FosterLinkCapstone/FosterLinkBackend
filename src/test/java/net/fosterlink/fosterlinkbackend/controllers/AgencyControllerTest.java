package net.fosterlink.fosterlinkbackend.controllers;

import com.google.maps.GeoApiContext;
import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ApproveAgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.GetAgenciesResponse;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.repositories.LocationRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AgencyMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
    private GeoApiContext geoApiContext;

    @InjectMocks
    private AgencyController agencyController;

    private UserEntity adminUser;
    private UserEntity nonAdminUser;

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
    }

    @Test
    void testGetAllAgencies_ReturnsOkWithGetAgenciesResponse() {
        // Arrange
        List<AgencyResponse> agencies = Collections.singletonList(new AgencyResponse());
        when(agencyMapper.getAllApprovedAgencies(0)).thenReturn(agencies);
        when(agencyRepository.countApproved()).thenReturn(25);

        // Act
        ResponseEntity<?> response = agencyController.getAllAgencies(0);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(GetAgenciesResponse.class, response.getBody());
        GetAgenciesResponse body = (GetAgenciesResponse) response.getBody();
        assertEquals(agencies, body.getAgencies());
        // Pagination: 25 total items, 10 per page -> 3 pages
        assertEquals(3, body.getTotalPages());
        verify(agencyMapper, times(1)).getAllApprovedAgencies(0);
    }

    @Test
    void testGetPendingAgencies_Admin_ReturnsOkWithGetAgenciesResponse() {
        // Arrange
        List<AgencyResponse> pending = Collections.singletonList(new AgencyResponse());
        when(agencyMapper.getAllPendingAgencies(0)).thenReturn(pending);
        when(agencyRepository.countPendingOrDenied()).thenReturn(5);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("admin@example.com");
            when(userRepository.findByEmail("admin@example.com")).thenReturn(adminUser);

            // Act
            ResponseEntity<?> response = agencyController.getPendingAgencies(0);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(GetAgenciesResponse.class, response.getBody());
            GetAgenciesResponse body = (GetAgenciesResponse) response.getBody();
            assertEquals(pending, body.getAgencies());
            // Pagination: 5 items, 10 per page -> 1 page
            assertEquals(1, body.getTotalPages());
            verify(agencyMapper, times(1)).getAllPendingAgencies(0);
        }
    }

    @Test
    void testGetPendingAgencies_NonAdmin_ReturnsForbidden() {
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(nonAdminUser);

            // Act
            ResponseEntity<?> response = agencyController.getPendingAgencies(0);

            // Assert
            assertEquals(403, response.getStatusCode().value());
            verify(agencyMapper, never()).getAllPendingAgencies(anyInt());
        }
    }

    @Test
    void testCountPending_Admin_ReturnsOkWithCount() {
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("admin@example.com");
            when(userRepository.findByEmail("admin@example.com")).thenReturn(adminUser);
            when(agencyRepository.countByApprovedNull()).thenReturn(5L);

            // Act
            ResponseEntity<?> response = agencyController.countPending();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(5L, response.getBody());
            verify(agencyRepository, times(1)).countByApprovedNull();
        }
    }

    @Test
    void testCountPending_NonAdmin_ReturnsForbidden() {
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(nonAdminUser);

            // Act
            ResponseEntity<?> response = agencyController.countPending();

            // Assert
            assertEquals(403, response.getStatusCode().value());
            verify(agencyRepository, never()).countByApprovedNull();
        }
    }

    @Test
    void testApproveAgency_Admin_AgencyExists_ReturnsOk() {
        // Arrange
        ApproveAgencyResponse model = new ApproveAgencyResponse();
        model.setId(10);
        model.setApproved(true);
        AgencyEntity agency = new AgencyEntity();
        agency.setId(10);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("admin@example.com");
            when(userRepository.findByEmail("admin@example.com")).thenReturn(adminUser);
            when(agencyRepository.findById(10)).thenReturn(Optional.of(agency));
            when(agencyRepository.save(any(AgencyEntity.class))).thenReturn(agency);

            // Act
            ResponseEntity<?> response = agencyController.approveAgency(model);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(agencyRepository, times(1)).save(agency);
        }
    }

    @Test
    void testApproveAgency_Admin_AgencyNotFound_ReturnsNotFound() {
        ApproveAgencyResponse model = new ApproveAgencyResponse();
        model.setId(999);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("admin@example.com");
            when(userRepository.findByEmail("admin@example.com")).thenReturn(adminUser);
            when(agencyRepository.findById(999)).thenReturn(Optional.empty());

            // Act
            ResponseEntity<?> response = agencyController.approveAgency(model);

            // Assert
            assertEquals(404, response.getStatusCode().value());
            verify(agencyRepository, never()).save(any(AgencyEntity.class));
        }
    }

    @Test
    void testApproveAgency_NonAdmin_ReturnsForbidden() {
        ApproveAgencyResponse model = new ApproveAgencyResponse();
        model.setId(10);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(nonAdminUser);

            // Act
            ResponseEntity<?> response = agencyController.approveAgency(model);

            // Assert
            assertEquals(403, response.getStatusCode().value());
            verify(agencyRepository, never()).findById(anyInt());
        }
    }
}
