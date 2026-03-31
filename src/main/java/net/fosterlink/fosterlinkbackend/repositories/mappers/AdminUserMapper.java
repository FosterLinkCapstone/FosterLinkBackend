package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.models.rest.AdminUserResponse;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Date;

@Service
public class AdminUserMapper {

    public AdminUserResponse mapRow(Object[] row) {
        AdminUserResponse res = new AdminUserResponse();
        res.setId(((Number) row[0]).intValue());
        res.setFirstName((String) row[1]);
        res.setLastName((String) row[2]);
        res.setUsername((String) row[3]);
        res.setEmail((String) row[4]);
        res.setPhoneNumber((String) row[5]);
        res.setProfilePictureUrl((String) row[6]);
        res.setAdministrator(toBool(row[7]));
        res.setFaqAuthor(toBool(row[8]));
        res.setVerifiedAgencyRep(toBool(row[9]));
        res.setVerifiedFoster(toBool(row[10]));
        res.setIdVerified(toBool(row[11]));
        res.setBannedAt((Date) row[12]);
        res.setRestrictedAt((Date) row[13]);
        res.setRestrictedUntil((Date) row[14]);
        res.setPostCount(toInt(row[15]));
        res.setReplyCount(toInt(row[16]));
        res.setAgencyCount(toInt(row[17]));
        res.setFaqAnswerCount(toInt(row[18]));
        res.setFaqSuggestionCount(toInt(row[19]));
        res.setPendingDeletion(toBool(row[20]));
        return res;
    }

    private boolean toBool(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        if (value instanceof byte[] bytes) return bytes.length > 0 && bytes[0] != 0;
        return false;
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof BigInteger bi) return bi.intValue();
        return 0;
    }

}
