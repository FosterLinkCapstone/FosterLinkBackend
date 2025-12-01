package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import net.fosterlink.fosterlinkbackend.models.rest.ApprovalStatus;
import net.fosterlink.fosterlinkbackend.models.rest.FaqResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PendingFaqResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.repositories.FAQRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class FaqMapper {

    private final FAQRepository fAQRepository;
    private final UserMapper userMapper;

    public FaqMapper(FAQRepository fAQRepository, UserMapper userMapper) {
        this.fAQRepository = fAQRepository;
        this.userMapper = userMapper;
    }

    public FaqResponse mapEntity(FaqEntity faqEntity, boolean isPreview) {

        FaqResponse faqResponse = new FaqResponse();

        faqResponse.setId(faqEntity.getId());
        faqResponse.setTitle(faqEntity.getTitle());
        faqResponse.setSummary(faqEntity.getSummary());
        faqResponse.setAuthor(new UserResponse(faqEntity.getAuthor()));
        faqResponse.setCreatedAt(faqEntity.getCreatedAt());
        faqResponse.setUpdatedAt(faqEntity.getUpdatedAt());

        return faqResponse;
    }

    public List<FaqResponse> allApprovedPreviews() {
        List<FaqResponse> faqResponseList = new ArrayList<>();
        List<Object[]> map = fAQRepository.allApprovedPreviews();

        for (Object[] obj : map) {
            FaqResponse faqResponse = new FaqResponse();
            faqResponse.setId((Integer)obj[0]);
            faqResponse.setTitle((String)obj[1]);
            faqResponse.setSummary((String)obj[2]);
            faqResponse.setCreatedAt((Date) obj[3]);
            faqResponse.setUpdatedAt((Date) obj[4]);
            faqResponse.setApproved(((Integer) obj[5]) == 1);
            faqResponse.setApproved_by_username((String)obj[6]);
            faqResponse.setAuthor(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 7,16)));
            faqResponseList.add(faqResponse);
        }
        return faqResponseList;
    }
    public List<PendingFaqResponse> allPendingPreviews() {
        List<PendingFaqResponse> faqResponseList = new ArrayList<>();
        List<Object[]> map = fAQRepository.allPendingPreviews();
        for (Object[] obj : map) {
            PendingFaqResponse faqResponse = new PendingFaqResponse();
            faqResponse.setId((Integer)obj[0]);
            faqResponse.setTitle((String)obj[1]);
            faqResponse.setSummary((String)obj[2]);
            faqResponse.setCreatedAt((Date) obj[3]);
            faqResponse.setUpdatedAt((Date) obj[4]);
            faqResponse.setApprovalStatus(ApprovalStatus.fromDbVal((Integer) obj[5]));
            faqResponse.setAuthor(userMapper.mapUserResponse(Arrays.copyOfRange(obj, 6,15)));
            faqResponseList.add(faqResponse);
        }
        return faqResponseList;
    }

}
