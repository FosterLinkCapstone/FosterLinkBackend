package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import net.fosterlink.fosterlinkbackend.models.rest.*;
import net.fosterlink.fosterlinkbackend.repositories.FAQApprovalRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRequestRepository;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class FaqMapper {

    private final FAQRepository fAQRepository;
    private final UserMapper userMapper;
    private final FAQApprovalRepository fAQApprovalRepository;
    private final FAQRequestRepository fAQRequestRepository;

    public FaqMapper(FAQRepository fAQRepository, UserMapper userMapper, FAQApprovalRepository fAQApprovalRepository, FAQRequestRepository fAQRequestRepository) {
        this.fAQRepository = fAQRepository;
        this.userMapper = userMapper;
        this.fAQApprovalRepository = fAQApprovalRepository;
        this.fAQRequestRepository = fAQRequestRepository;
    }

    public FaqResponse mapNewFaq(FaqEntity faqEntity) {

        FaqResponse faqResponse = new FaqResponse();

        faqResponse.setId(faqEntity.getId());
        faqResponse.setTitle(faqEntity.getTitle());
        faqResponse.setSummary(faqEntity.getSummary());
        faqResponse.setAuthor(new UserResponse(faqEntity.getAuthor()));
        faqResponse.setCreatedAt(faqEntity.getCreatedAt());
        faqResponse.setUpdatedAt(faqEntity.getUpdatedAt());

        faqResponse.setApprovedByUsername(null);
        faqResponse.setApproved(false);

        return faqResponse;
    }

    public List<FaqResponse> allApprovedPreviewsForUser(int userId, int pageNumber) {
        List<FaqResponse> faqResponseList = new ArrayList<>();
        List<Object[]> map = fAQRepository.allApprovedPreviewsForUser(userId, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));

        return mapFaqResponses(faqResponseList, map);
    }

    private List<FaqResponse> mapFaqResponses(List<FaqResponse> faqResponseList, List<Object[]> map) {
        for (Object[] obj : map) {
            FaqResponse faqResponse = new FaqResponse();
            faqResponse.setId((Integer)obj[0]);
            faqResponse.setTitle((String)obj[1]);
            faqResponse.setSummary((String)obj[2]);
            faqResponse.setCreatedAt((Date) obj[3]);
            faqResponse.setUpdatedAt((Date) obj[4]);
            faqResponse.setApproved(((Integer) obj[5]) == 1);
            faqResponse.setApprovedByUsername((String)obj[6]);
            faqResponse.setAuthor(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 7,16)));
            faqResponseList.add(faqResponse);
        }
        return faqResponseList;
    }

    public List<FaqResponse> allApprovedPreviews(int pageNumber) {
        List<FaqResponse> faqResponseList = new ArrayList<>();
        List<Object[]> map = fAQRepository.allApprovedPreviews(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));

        return mapFaqResponses(faqResponseList, map);
    }
    public List<PendingFaqResponse> allPendingPreviews(int pageNumber) {
        List<PendingFaqResponse> faqResponseList = new ArrayList<>();
        List<Object[]> map = fAQRepository.allPendingPreviews(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        for (Object[] obj : map) {
            PendingFaqResponse faqResponse = new PendingFaqResponse();
            faqResponse.setId((Integer)obj[0]);

            faqResponse.setTitle((String)obj[1]);
            faqResponse.setSummary((String)obj[2]);
            faqResponse.setCreatedAt((Date) obj[3]);
            faqResponse.setUpdatedAt((Date) obj[4]);
            faqResponse.setApprovalStatus(ApprovalStatus.fromDbVal(Integer.parseInt(String.valueOf(obj[5])))); // lol
            faqResponse.setDeniedByUsername((String)obj[6]);
            faqResponse.setAuthor(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 7,16)));
            faqResponseList.add(faqResponse);
        }
        return faqResponseList;
    }
    public ApprovalCheckResponse checkApprovalStatusForUser(int userId) {
        List<Object[]> toMap = fAQApprovalRepository.getApprovalCountsForUser(userId);
        ApprovalCheckResponse approvalCheckResponse = new ApprovalCheckResponse();
        approvalCheckResponse.setCountPending(Integer.parseInt(String.valueOf(toMap.get(0)[0])));
        approvalCheckResponse.setCountDenied(Integer.parseInt(String.valueOf(toMap.get(0)[1])));
        return approvalCheckResponse;
    }
    public List<FaqRequestResponse> getAllRequests() {
        List<FaqRequestResponse> faqRequestResponseList = new ArrayList<>();
        List<Object[]> toMap = fAQRequestRepository.getAllRequests();
        for (Object[] obj : toMap) {
            FaqRequestResponse faqRequestResponse = new FaqRequestResponse();
            faqRequestResponse.setId((Integer)obj[0]);
            faqRequestResponse.setSuggestion((String) obj[1]);
            faqRequestResponse.setSuggestingUsername((String) obj[2]);
            faqRequestResponseList.add(faqRequestResponse);
        }
        return faqRequestResponseList;
    }
}
