package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.ApprovalCheckResponse;
import net.fosterlink.fosterlinkbackend.models.rest.FaqResponse;
import net.fosterlink.fosterlinkbackend.models.rest.GetFaqsResponse;
import net.fosterlink.fosterlinkbackend.models.rest.GetPendingFaqsResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PendingFaqResponse;
import net.fosterlink.fosterlinkbackend.repositories.FAQApprovalRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.FaqMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaqControllerTest {

    @Mock
    private FAQRepository fAQRepository;

    @Mock
    private FaqMapper faqMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FAQApprovalRepository fAQApprovalRepository;

    @Mock
    private FAQRequestRepository fAQRequestRepository;

    @InjectMocks
    private FaqController faqController;

    private UserEntity adminUser;
    private UserEntity faqAuthorUser;

    @BeforeEach
    void setUp() {
        adminUser = new UserEntity();
        adminUser.setId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setAdministrator(true);
        adminUser.setFaqAuthor(false);

        faqAuthorUser = new UserEntity();
        faqAuthorUser.setId(2);
        faqAuthorUser.setEmail("author@example.com");
        faqAuthorUser.setAdministrator(false);
        faqAuthorUser.setFaqAuthor(true);
    }

    @Test
    void testGetAllFaqs_ReturnsOkWithGetFaqsResponse() {
        List<FaqResponse> faqs = Collections.singletonList(new FaqResponse());
        when(faqMapper.allApprovedPreviews(0)).thenReturn(faqs);
        when(fAQRepository.countApproved()).thenReturn(25);

        ResponseEntity<?> response = faqController.getAllFaqs(0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(GetFaqsResponse.class, response.getBody());
        GetFaqsResponse body = (GetFaqsResponse) response.getBody();
        assertEquals(faqs, body.getFaqs());
        // Pagination: 25 total items, 10 per page -> 3 pages
        assertEquals(3, body.getTotalPages());
        verify(faqMapper, times(1)).allApprovedPreviews(0);
    }

    @Test
    void testGetContentFor_FaqExists_ReturnsContent() {
        FaqEntity faq = new FaqEntity();
        faq.setId(1);
        faq.setContent("FAQ content here");
        when(fAQRepository.findById(1)).thenReturn(Optional.of(faq));

        ResponseEntity<?> response = faqController.getContentFor(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("FAQ content here", response.getBody());
        verify(fAQRepository, times(1)).findById(1);
    }

    @Test
    void testGetContentFor_FaqNotFound_ReturnsNotFound() {
        when(fAQRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity<?> response = faqController.getContentFor(999);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(fAQRepository, times(1)).findById(999);
    }

    @Test
    void testGetPendingFaqs_Admin_ReturnsOkWithGetPendingFaqsResponse() {
        List<PendingFaqResponse> pending = Collections.singletonList(new PendingFaqResponse());
        when(faqMapper.allPendingPreviews(0)).thenReturn(pending);
        when(fAQRepository.countPending()).thenReturn(5);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(true);
            when(userRepository.findByEmail("admin@example.com")).thenReturn(adminUser);
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("admin@example.com");

            ResponseEntity<?> response = faqController.getPendingFaqs(0);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(GetPendingFaqsResponse.class, response.getBody());
            GetPendingFaqsResponse body = (GetPendingFaqsResponse) response.getBody();
            assertEquals(pending, body.getFaqs());
            // Pagination: 5 items, 10 per page -> 1 page
            assertEquals(1, body.getTotalPages());
            verify(faqMapper, times(1)).allPendingPreviews(0);
        }
    }

    @Test
    void testGetPendingFaqs_NonAdmin_ReturnsForbidden() {
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(true);
            when(userRepository.findByEmail("user@example.com")).thenReturn(faqAuthorUser);
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("user@example.com");

            ResponseEntity<?> response = faqController.getPendingFaqs(0);

            assertEquals(403, response.getStatusCode().value());
            verify(faqMapper, never()).allPendingPreviews(anyInt());
        }
    }

    @Test
    void testGetUnapprovedFaqCount_LoggedIn_ReturnsApprovalStatus() {
        ApprovalCheckResponse status = new ApprovalCheckResponse(2, 1);
        when(faqMapper.checkApprovalStatusForUser(1)).thenReturn(status);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(true);
            when(userRepository.findByEmail("admin@example.com")).thenReturn(adminUser);
            jwtUtilMock.when(JwtUtil::getLoggedInEmail).thenReturn("admin@example.com");

            ResponseEntity<?> response = faqController.getUnapprovedFaqCount();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ApprovalCheckResponse.class, response.getBody());
            ApprovalCheckResponse body = (ApprovalCheckResponse) response.getBody();
            assertEquals(2, body.getCountPending());
            assertEquals(1, body.getCountDenied());
        }
    }

    @Test
    void testGetUnapprovedFaqCount_NotLoggedIn_ReturnsZeroCounts() {
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(false);

            ResponseEntity<?> response = faqController.getUnapprovedFaqCount();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ApprovalCheckResponse.class, response.getBody());
            ApprovalCheckResponse body = (ApprovalCheckResponse) response.getBody();
            assertEquals(0, body.getCountPending());
            assertEquals(0, body.getCountDenied());
            verify(faqMapper, never()).checkApprovalStatusForUser(anyInt());
        }
    }

    @Test
    void testGetAllAuthor_UserExistsWithFaqs_ReturnsOkWithList() {
        List<FaqResponse> faqs = Collections.singletonList(new FaqResponse());
        when(userRepository.existsById(1)).thenReturn(true);
        when(faqMapper.allApprovedPreviewsForUser(1, 0)).thenReturn(faqs);

        ResponseEntity<?> response = faqController.getAllAuthor(1, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(faqs, response.getBody());
        verify(faqMapper, times(1)).allApprovedPreviewsForUser(1, 0);
    }

    @Test
    void testGetAllAuthor_UserNotFound_ReturnsNotFound() {
        when(userRepository.existsById(999)).thenReturn(false);

        ResponseEntity<?> response = faqController.getAllAuthor(999, 0);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(faqMapper, never()).allApprovedPreviewsForUser(anyInt(), anyInt());
    }

    @Test
    void testGetAllAuthor_UserExistsButNoFaqs_ReturnsNotFound() {
        when(userRepository.existsById(1)).thenReturn(true);
        when(faqMapper.allApprovedPreviewsForUser(1, 0)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = faqController.getAllAuthor(1, 0);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
