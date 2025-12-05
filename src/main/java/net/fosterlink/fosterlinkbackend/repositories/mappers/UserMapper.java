package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserMapper {

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

}
