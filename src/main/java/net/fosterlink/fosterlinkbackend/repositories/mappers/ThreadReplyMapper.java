package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.models.rest.PostMetadataResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadReplyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.repositories.ThreadReplyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThreadReplyMapper {

    private @Autowired ThreadReplyRepository threadReplyRepository;
    private @Autowired UserMapper userMapper;

    private ThreadReplyResponse mapThreadReply(Object[] row) {
        ThreadReplyResponse response = new ThreadReplyResponse();
        response.setId((Integer) row[0]);
        response.setContent((String) row[1]);
        response.setCreatedAt((Date) row[2]);
        response.setUpdatedAt((Date) row[3]);
        response.setLikeCount(((Number) row[4]).intValue());
        response.setLiked((Integer) row[5] == 1);

        UserResponse userResponse = userMapper.mapUserResponse(Arrays.copyOfRange(row, 6, 17));

        response.setAuthor(userResponse);

        return response;
    }

    private ThreadReplyResponse mapThreadReplyWithMetadata(Object[] row) {
        ThreadReplyResponse response = mapThreadReply(row);

        PostMetadataResponse metadata = new PostMetadataResponse(
            ((Number) row[17]).intValue(),
            (Boolean) row[18],
            (Boolean) row[19],
            (Boolean) row[20],
            (Boolean) row[21],
            (String) row[22]
        );
        response.setPostMetadata(metadata);

        return response;
    }

    public List<ThreadReplyResponse> getRepliesForThread(int threadId, int userId) {
        List<Object[]> results = threadReplyRepository.getRepliesForThread(threadId, userId);
        return results.stream().map(this::mapThreadReply).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<ThreadReplyResponse> getAllRepliesForThreadAdmin(int threadId, int userId) {
        List<Object[]> results = threadReplyRepository.getAllRepliesForThreadAdmin(threadId, userId);
        return results.stream().map(this::mapThreadReplyWithMetadata).collect(Collectors.toCollection(ArrayList::new));
    }

}
