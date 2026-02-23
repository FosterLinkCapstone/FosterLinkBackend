package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.GetThreadsResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadReplyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadResponse;
import net.fosterlink.fosterlinkbackend.repositories.*;
import net.fosterlink.fosterlinkbackend.repositories.mappers.ThreadMapper;
import net.fosterlink.fosterlinkbackend.repositories.mappers.ThreadReplyMapper;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadControllerTest {

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private ThreadTagRepository threadTagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ThreadLikeRepository threadLikeRepository;

    @Mock
    private ThreadMapper threadMapper;

    @Mock
    private ThreadReplyMapper threadReplyMapper;

    @Mock
    private ThreadReplyLikeRepository threadReplyLikeRepository;

    @Mock
    private PostMetadataRepository postMetadataRepository;

    @Mock
    private ThreadReplyRepository threadReplyRepository;

    @InjectMocks
    private ThreadController threadController;

    private UserEntity testUser;
    private ThreadResponse threadResponse;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1);
        testUser.setEmail("user@example.com");

        threadResponse = new ThreadResponse();
        threadResponse.setId(1);
        threadResponse.setTitle("Test Thread");
    }

    @Test
    void testSearchById_ThreadExists_ReturnsOk() {
        when(threadMapper.findById(eq(1), anyInt())).thenReturn(threadResponse);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(false);

            ResponseEntity<?> response = threadController.searchById(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertInstanceOf(ThreadResponse.class, response.getBody());
            assertEquals(1, ((ThreadResponse) response.getBody()).getId());
        }
    }

    @Test
    void testSearchById_ThreadNotFound_ReturnsNotFound() {
        when(threadMapper.findById(eq(999), anyInt())).thenReturn(null);

        ResponseEntity<?> response = threadController.searchById(999);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetThreads_ReturnsOkWithGetThreadsResponse() {
        List<ThreadResponse> threads = Collections.singletonList(threadResponse);
        when(threadMapper.getThreads(eq("newest"), anyInt(), eq(0))).thenReturn(threads);
        when(threadRepository.countVisible()).thenReturn(25);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(false);

            ResponseEntity<?> response = threadController.getThreads("newest", 0);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(GetThreadsResponse.class, response.getBody());
            GetThreadsResponse body = (GetThreadsResponse) response.getBody();
            assertNotNull(body.getThreads());
            assertEquals(1, body.getThreads().size());
            // Pagination: 25 total items, 10 per page -> 3 pages
            assertEquals(3, body.getTotalPages());
        }
    }

    @Test
    void testRand_NoUserId_ReturnsOkWithThreads() {
        List<ThreadResponse> threads = Collections.singletonList(threadResponse);
        when(threadMapper.findRandomWeightedThreads(-1)).thenReturn(threads);

        ResponseEntity<?> response = threadController.rand(-1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(threads, response.getBody());
        verify(threadMapper, times(1)).findRandomWeightedThreads(-1);
        verify(threadMapper, never()).findRandomWeightedThreadsForUser(anyInt());
    }

    @Test
    void testRand_WithUserId_ReturnsOkWithWeightedThreads() {
        List<ThreadResponse> threads = Collections.singletonList(threadResponse);
        when(threadMapper.findRandomWeightedThreadsForUser(1)).thenReturn(threads);

        ResponseEntity<?> response = threadController.rand(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(threadMapper, times(1)).findRandomWeightedThreadsForUser(1);
    }

    @Test
    void testSearchByUser_UserExists_ReturnsOkWithGetThreadsResponse() {
        List<ThreadResponse> threads = Collections.singletonList(threadResponse);
        when(userRepository.existsById(1)).thenReturn(true);
        when(threadMapper.searchByUser(anyInt(), eq(1), eq(0))).thenReturn(threads);
        when(threadRepository.visibleThreadCountForUser(1)).thenReturn(5);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(false);

            ResponseEntity<?> response = threadController.searchByUser(1, 0);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(GetThreadsResponse.class, response.getBody());
            GetThreadsResponse body = (GetThreadsResponse) response.getBody();
            assertEquals(threads, body.getThreads());
            // Pagination: 5 items, 10 per page -> 1 page
            assertEquals(1, body.getTotalPages());
        }
    }

    @Test
    void testSearchByUser_UserNotFound_ReturnsNotFound() {
        when(userRepository.existsById(999)).thenReturn(false);

        ResponseEntity<?> response = threadController.searchByUser(999, 0);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(threadMapper, never()).searchByUser(anyInt(), anyInt(), anyInt());
    }

    @Test
    void testReplies_ReturnsOkWithReplyList() {
        List<ThreadReplyResponse> replies = Collections.singletonList(new ThreadReplyResponse());
        when(threadReplyMapper.getRepliesForThread(eq(1), anyInt())).thenReturn(replies);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(JwtUtil::isLoggedIn).thenReturn(false);

            ResponseEntity<?> response = threadController.replies(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(replies, response.getBody());
        }
    }
}
