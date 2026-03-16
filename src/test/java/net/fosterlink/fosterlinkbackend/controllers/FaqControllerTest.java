package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import net.fosterlink.fosterlinkbackend.entities.FAQApprovalEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.models.rest.ApprovalCheckResponse;
import net.fosterlink.fosterlinkbackend.models.rest.FaqResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PaginatedResponse;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private LoggedInUser loggedInAdmin;

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

        loggedInAdmin = new LoggedInUser(1, "admin@example.com", 0, "", Set.of("ADMINISTRATOR"), true, true, true, true, false);
    }

    @Test
    void testGetAllFaqs_ReturnsOkWithPaginatedResponse() {
        List<FaqResponse> faqs = Collections.singletonList(new FaqResponse());
        when(faqMapper.allApprovedPreviewsWithSearch(eq(0), isNull(), isNull())).thenReturn(faqs);
        when(faqMapper.countApprovedWithSearch(isNull(), isNull())).thenReturn(25);

        ResponseEntity<?> response = faqController.getAllFaqs(0, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(PaginatedResponse.class, response.getBody());
        PaginatedResponse<?> body = (PaginatedResponse<?>) response.getBody();
        assertEquals(faqs, body.getItems());
        // Pagination: 25 total items, 10 per page -> 3 pages
        assertEquals(3, body.getTotalPages());
        verify(faqMapper, times(1)).allApprovedPreviewsWithSearch(eq(0), isNull(), isNull());
    }

    @Test
    void testGetContentFor_FaqExists_ReturnsContent() {
        FaqEntity faq = new FaqEntity();
        faq.setId(1);
        faq.setContent("FAQ content here");
        FAQApprovalEntity approval = new FAQApprovalEntity();
        approval.setApproved(true);
        approval.setHiddenBy(null);
        when(fAQRepository.findById(1)).thenReturn(Optional.of(faq));
        when(fAQApprovalRepository.findFAQApprovalEntityByFaqId(1)).thenReturn(Optional.of(approval));

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
    void testGetPendingFaqs_Admin_ReturnsOkWithPaginatedResponse() {
        List<PendingFaqResponse> pending = Collections.singletonList(new PendingFaqResponse());
        when(faqMapper.allPendingPreviews(0)).thenReturn(pending);
        when(fAQRepository.countPending()).thenReturn(5);

        ResponseEntity<?> response = faqController.getPendingFaqs(0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(PaginatedResponse.class, response.getBody());
        PaginatedResponse<?> body = (PaginatedResponse<?>) response.getBody();
        assertEquals(pending, body.getItems());
        // Pagination: 5 items, 10 per page -> 1 page
        assertEquals(1, body.getTotalPages());
        verify(faqMapper, times(1)).allPendingPreviews(0);
    }

    // Note: getPendingFaqs is protected by @PreAuthorize("hasAuthority('ADMINISTRATOR')").
    // In a unit test without Spring Security, the method runs; 403 is enforced in integration tests.

    @Test
    void testGetUnapprovedFaqCount_LoggedIn_ReturnsApprovalStatus() {
        ApprovalCheckResponse status = new ApprovalCheckResponse(2, 1);
        when(faqMapper.checkApprovalStatusForUser(1)).thenReturn(status);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(loggedInAdmin);

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
            jwtUtilMock.when(JwtUtil::getLoggedInUser).thenReturn(null);

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
    void testGetAllAuthor_UserExistsWithFaqs_ReturnsOkWithPaginatedResponse() {
        List<FaqResponse> faqs = Collections.singletonList(new FaqResponse());
        UserEntity author = new UserEntity();
        author.setId(1);
        author.setAccountDeleted(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(fAQRepository.countApprovedByAuthor(1)).thenReturn(1);
        when(faqMapper.allApprovedPreviewsForUser(1, 0)).thenReturn(faqs);

        ResponseEntity<?> response = faqController.getAllAuthor(1, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(PaginatedResponse.class, response.getBody());
        PaginatedResponse<?> body = (PaginatedResponse<?>) response.getBody();
        assertEquals(faqs, body.getItems());
        assertEquals(1, body.getTotalPages());
        verify(faqMapper, times(1)).allApprovedPreviewsForUser(1, 0);
    }

    @Test
    void testGetAllAuthor_UserNotFound_ReturnsNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity<?> response = faqController.getAllAuthor(999, 0);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(faqMapper, never()).allApprovedPreviewsForUser(anyInt(), anyInt());
    }

    @Test
    void testGetAllAuthor_UserExistsButNoFaqs_ReturnsOkWithEmptyItems() {
        UserEntity author = new UserEntity();
        author.setId(1);
        author.setAccountDeleted(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(fAQRepository.countApprovedByAuthor(1)).thenReturn(0);
        when(faqMapper.allApprovedPreviewsForUser(1, 0)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = faqController.getAllAuthor(1, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(PaginatedResponse.class, response.getBody());
        PaginatedResponse<?> body = (PaginatedResponse<?>) response.getBody();
        assertTrue(body.getItems().isEmpty());
    }
}
