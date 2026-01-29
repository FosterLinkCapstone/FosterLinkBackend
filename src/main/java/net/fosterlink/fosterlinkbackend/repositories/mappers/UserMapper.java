package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ProfileMetadataResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserMapper {

    private @Autowired UserRepository userRepository;
    private @Autowired AgencyRepository agencyRepository;

    public UserResponse mapUserResponse(Object[] row) {
        UserResponse author = new UserResponse();
        author.setId((Integer) row[0]);
        author.setFullName(row[1] + " " + row[2]);
        author.setUsername((String) row[3]);
        author.setProfilePictureUrl((String) row[4]);
        author.setVerified(
                (Boolean) row[5] || // verified_foster
                        (Boolean) row[6] || // faq_author
                        (Boolean) row[7]    // verified_agency_rep
        );
        author.setCreatedAt((Date) row[8]);
        return author;
    }

    public ProfileMetadataResponse mapProfileMetadataResponse(int userId) {
        Object[] row = userRepository.getProfileMetadataRow(userId).get(0);
        if (row == null) return null;
        ProfileMetadataResponse res = new ProfileMetadataResponse();
        res.setUserId((Integer) row[0]);
        res.setAdmin((Boolean) row[1]);
        res.setFaqAuthor((Boolean) row[2]);
        res.setAgencyId(row[4] == null ? null : String.valueOf((Integer) row[4]));
        res.setAgencyName((String) row[5]);

        // Query all agencies for this user
        List<net.fosterlink.fosterlinkbackend.entities.AgencyEntity> agencies = agencyRepository.findByAgentId(userId);
        res.setAgencyCount(agencies.size());
        if (!agencies.isEmpty()) {
            res.setFirstAgencyName(agencies.get(0).getName());
            // Set agencyName to first agency name for backward compatibility
            if (res.getAgencyName() == null) {
                res.setAgencyName(agencies.get(0).getName());
            }
        } else {
            res.setFirstAgencyName(null);
        }

        // Create UserResponse from the row data
        UserResponse userResponse = new UserResponse();
        userResponse.setId((Integer) row[0]);
        userResponse.setFullName(row[6] + " " + row[7]); // firstName + lastName
        userResponse.setUsername((String) row[8]);
        userResponse.setProfilePictureUrl((String) row[9]);
        userResponse.setVerified(
                (Boolean) row[10] || // verifiedFoster
                        (Boolean) row[2] || // faqAuthor
                        (Boolean) row[3]    // verifiedAgencyRep
        );
        userResponse.setCreatedAt((Date) row[11]);
        res.setUser(userResponse);

        return res;
    }

}
