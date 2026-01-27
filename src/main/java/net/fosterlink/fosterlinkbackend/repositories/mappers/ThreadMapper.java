package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.models.rest.ThreadResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.repositories.ThreadRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ThreadResponse mapThread(Object[] row) {
        ThreadResponse response = new ThreadResponse();
        response.setId((Integer) row[0]);
        response.setTitle((String) row[1]);
        response.setContent((String) row[2]);
        response.setCreatedAt((Date) row[3]);
        response.setUpdatedAt((Date) row[4]);
        response.setLikeCount(((Number) row[5]).intValue());
        response.setLiked((Integer) row[6] == 1);

        UserResponse author = userMapper.mapUserResponse(Arrays.copyOfRange(row, 7, 16));

        response.setAuthor(author);

        String tagsString = (String) row[16];
        if (tagsString != null && !tagsString.isEmpty()) {
            response.setTags(Arrays.asList(tagsString.split(",")));
        } else {
            response.setTags(new ArrayList<>());
        }

        return response;
    }

}
