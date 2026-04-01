package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import net.fosterlink.fosterlinkbackend.models.rest.*;
import net.fosterlink.fosterlinkbackend.repositories.FAQApprovalRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRequestRepository;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import org.springframework.cache.annotation.Cacheable;
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
            // Only include approved FAQs; skip denied or pending (approved column is index 5)
            if (obj.length <= 5 || !Integer.valueOf(1).equals(obj[5])) {
                continue;
            }
            FaqResponse faqResponse = new FaqResponse();
            faqResponse.setId((Integer)obj[0]);
            faqResponse.setTitle((String)obj[1]);
            faqResponse.setSummary((String)obj[2]);
            faqResponse.setCreatedAt((Date) obj[3]);
            faqResponse.setUpdatedAt((Date) obj[4]);
            faqResponse.setApproved(true);
            faqResponse.setApprovedByUsername((String)obj[6]);
            faqResponse.setAuthor(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 7, 18)));
            faqResponseList.add(faqResponse);
        }
        return faqResponseList;
    }

    // Key is pageNumber only; page size is fixed at SqlUtil.ITEMS_PER_PAGE.
    // If ITEMS_PER_PAGE changes, flush the faqApprovedPreviews cache before the new code goes live.
    @Cacheable(value = "faqApprovedPreviews", key = "#pageNumber")
    public List<FaqResponse> allApprovedPreviews(int pageNumber) {
        List<FaqResponse> faqResponseList = new ArrayList<>();
        List<Object[]> map = fAQRepository.allApprovedPreviews(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));

        return mapFaqResponses(faqResponseList, map);
    }

    /** Search: when search is null/empty, uses cached allApprovedPreviews. searchBy: authorFullName, authorUsername, title, summary, or null/all for any. */
    public List<FaqResponse> allApprovedPreviewsWithSearch(int pageNumber, String search, String searchBy) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        if (normalizedSearch == null) {
            return allApprovedPreviews(pageNumber);
        }
        String normalizedSearchBy = (searchBy != null && !searchBy.isBlank()) ? searchBy.trim() : null;
        List<FaqResponse> faqResponseList = new ArrayList<>();
        List<Object[]> map = fAQRepository.allApprovedPreviewsWithSearchNewest(normalizedSearch, normalizedSearchBy, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return mapFaqResponses(faqResponseList, map);
    }

    public int countApprovedWithSearch(String search, String searchBy) {
        if (search == null || search.isBlank()) {
            return fAQRepository.countApproved();
        }
        String normalizedSearchBy = (searchBy != null && !searchBy.isBlank()) ? searchBy.trim() : null;
        return fAQRepository.countApprovedWithSearch(search.trim(), normalizedSearchBy);
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
            faqResponse.setUpdatedAt(obj[4] != null ? (Date) obj[4] : null);
            faqResponse.setApprovalStatus(ApprovalStatus.fromDbVal(Integer.parseInt(String.valueOf(obj[5])))); // lol
            faqResponse.setDeniedByUsername((String)obj[6]);
            faqResponse.setAuthor(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 7, 18)));
            faqResponseList.add(faqResponse);
        }
        return faqResponseList;
    }
    public ApprovalCheckResponse checkApprovalStatusForUser(int userId) {
        List<Object[]> toMap = fAQApprovalRepository.getApprovalCountsForUser(userId);
        ApprovalCheckResponse approvalCheckResponse = new ApprovalCheckResponse();
        Object pending = toMap.get(0)[0];
        Object denied = toMap.get(0)[1];
        approvalCheckResponse.setCountPending(pending != null ? Integer.parseInt(String.valueOf(pending)) : 0);
        approvalCheckResponse.setCountDenied(denied != null ? Integer.parseInt(String.valueOf(denied)) : 0);
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

    public List<HiddenFaqResponse> allHiddenByAdminPreviews(int pageNumber) {
        List<Object[]> rows = fAQRepository.allHiddenByAdminPreviews(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return mapHiddenFaqResponses(rows);
    }

    public List<HiddenFaqResponse> allHiddenByUserPreviews(int pageNumber) {
        List<Object[]> rows = fAQRepository.allHiddenByUserPreviews(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return mapHiddenFaqResponses(rows);
    }

    private List<HiddenFaqResponse> mapHiddenFaqResponses(List<Object[]> rows) {
        List<HiddenFaqResponse> result = new ArrayList<>();
        for (Object[] obj : rows) {
            HiddenFaqResponse r = new HiddenFaqResponse();
            r.setId((Integer) obj[0]);
            r.setTitle((String) obj[1]);
            r.setSummary((String) obj[2]);
            r.setCreatedAt((Date) obj[3]);
            r.setUpdatedAt((Date) obj[4]);
            r.setHiddenBy((String) obj[5]);
            r.setHiddenByAuthor(Boolean.TRUE.equals(obj[6]));
            r.setAuthor(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 7, 18)));
            result.add(r);
        }
        return result;
    }
}
