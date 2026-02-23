package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.models.rest.GetHiddenThreadsResponse;
import net.fosterlink.fosterlinkbackend.models.rest.HiddenThreadResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PostMetadataResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.repositories.ThreadRepository;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThreadMapper {

    @Autowired private ThreadRepository threadRepository;
    @Autowired private UserMapper userMapper;

    public List<ThreadResponse> findRandomWeightedThreads(int userId) {
        List<Object[]> results = threadRepository.findRandomWeightedThreads(userId);
        return results.stream().map(this::mapThread).collect(Collectors.toList());
    }
    public List<ThreadResponse> getThreads(String orderBy, int userId, int pageNumber) {

        // Normalize frontend/backcompat values.
        String normalized = orderBy == null ? "newest" : orderBy.trim().toLowerCase();
        if (normalized.equals("likes")) normalized = "most liked";

        List<Object[]> results = switch (normalized) {
            case "oldest" -> threadRepository.findThreadsOldest(userId, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
            case "most liked" -> threadRepository.findThreadsMostLiked(userId, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
            default -> threadRepository.findThreadsNewest(userId, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        };

        return results.stream().map(this::mapThread).collect(Collectors.toList());
    }

    public List<ThreadResponse> searchByUser(int userId, int authorId, int pageNumber) {

        List<Object[]> results = threadRepository.findThreadsForUser(userId, authorId, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return results.stream().map(this::mapThread).collect(Collectors.toList());

    }

    public ThreadResponse findById(int threadId, int userId) {
        List<Object[]> results = threadRepository.findByIdResponse(threadId, userId);
        if (results == null || results.isEmpty()) {
            return null;
        }
        Object[] result = results.get(0);
        return mapThread(result);
    }
    public List<ThreadResponse> findRandomWeightedThreadsForUser(int userId) {
        List<Object[]> results = threadRepository.findRandomWeightedThreadsForUser(userId);
        return results.stream().map(this::mapThread).collect(Collectors.toCollection(ArrayList::new));
    }
    public GetHiddenThreadsResponse getHiddenThreadsAdminDeleted(int pageNumber, int userId) {
        List<Object[]> results = threadRepository.getHiddenThreadsAdminDeleted(userId, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        List<HiddenThreadResponse> threads = results.stream().map(this::mapHiddenThread).collect(Collectors.toList());
        int totalCount = threadRepository.countHiddenThreadsAdminDeleted();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return new GetHiddenThreadsResponse(threads, totalPages);
    }

    public GetHiddenThreadsResponse getHiddenThreadsUserDeleted(int pageNumber, int userId) {
        List<Object[]> results = threadRepository.getHiddenThreadsUserDeleted(userId, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        List<HiddenThreadResponse> threads = results.stream().map(this::mapHiddenThread).collect(Collectors.toList());
        int totalCount = threadRepository.countHiddenThreadsUserDeleted();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return new GetHiddenThreadsResponse(threads, totalPages);
    }

    public HiddenThreadResponse findHiddenThreadById(int threadId, int userId) {
        List<Object[]> results = threadRepository.findHiddenThreadById(threadId, userId);
        if (results == null || results.isEmpty()) {
            return null;
        }
        return mapHiddenThread(results.get(0));
    }

    private static boolean toBoolean(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        return false;
    }

    private HiddenThreadResponse mapHiddenThread(Object[] row) {
        HiddenThreadResponse response = new HiddenThreadResponse();
        response.setId((Integer) row[0]);
        response.setTitle((String) row[1]);
        response.setContent((String) row[2]);
        response.setCreatedAt((Date) row[3]);
        response.setUpdatedAt((Date) row[4]);
        response.setLikeCount(((Number) row[5]).intValue());
        response.setLiked(toBoolean(row[6]));
        response.setCommentCount(((Number) row[7]).intValue());
        response.setUserPostCount(((Number) row[8]).intValue());

        UserResponse author = userMapper.mapUserResponse(Arrays.copyOfRange(row, 9, 18));
        response.setAuthor(author);

        PostMetadataResponse postMetadata = new PostMetadataResponse(
                ((Number) row[18]).intValue(),
                toBoolean(row[19]),
                toBoolean(row[20]),
                toBoolean(row[21]),
                toBoolean(row[22]),
                (String) row[23]
        );
        response.setPostMetadata(postMetadata);

        String tagsString = (String) row[24];
        if (tagsString != null && !tagsString.isEmpty()) {
            response.setTags(Arrays.asList(tagsString.split(",")));
        } else {
            response.setTags(new ArrayList<>());
        }

        return response;
    }

    private ThreadResponse mapThread(Object[] row) {
        ThreadResponse response = new ThreadResponse();
        response.setId((Integer) row[0]);
        response.setTitle((String) row[1]);
        response.setContent((String) row[2]);
        response.setCreatedAt((Date) row[3]);
        response.setUpdatedAt((Date) row[4]);
        response.setLikeCount(((Number) row[5]).intValue());
        response.setLiked((Integer) row[6] == 1);

        response.setCommentCount(((Number) row[7]).intValue());
        response.setUserPostCount(((Number) row[8]).intValue());

        UserResponse author = userMapper.mapUserResponse(Arrays.copyOfRange(row, 9, 18));

        response.setAuthor(author);

        String tagsString = (String) row[18];
        if (tagsString != null && !tagsString.isEmpty()) {
            response.setTags(Arrays.asList(tagsString.split(",")));
        } else {
            response.setTags(new ArrayList<>());
        }

        return response;
    }

}
