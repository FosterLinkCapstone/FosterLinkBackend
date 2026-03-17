package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.models.rest.AccountDeletionRequestResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.repositories.AccountDeletionRequestRepository;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AccountDeletionRequestMapper {

    private @Autowired AccountDeletionRequestRepository accountDeletionRequestRepository;

    public List<AccountDeletionRequestResponse> getAllPendingByRecency(int pageNumber) {
        List<Object[]> rows = accountDeletionRequestRepository.findAllPendingByRecency(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return mapRows(rows);
    }

    public List<AccountDeletionRequestResponse> getAllPendingByUrgency(int pageNumber) {
        List<Object[]> rows = accountDeletionRequestRepository.findAllPendingByUrgency(PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return mapRows(rows);
    }

    private List<AccountDeletionRequestResponse> mapRows(List<Object[]> rows) {
        List<AccountDeletionRequestResponse> results = new ArrayList<>();
        for (Object[] row : rows) {
            AccountDeletionRequestResponse response = new AccountDeletionRequestResponse();
            response.setId(((Number) row[0]).intValue());
            response.setRequestedAt((Date) row[1]);
            response.setAutoApproveBy((Date) row[2]);
            response.setReviewedAt((Date) row[3]);
            response.setAutoApproved(toBool(row[4]));
            response.setApproved(toBool(row[5]));
            response.setDelayNote((String) row[6]);
            response.setClearAccount(toBool(row[7]));

            UserResponse requestedBy = new UserResponse();
            requestedBy.setId(((Number) row[8]).intValue());
            requestedBy.setFullName(row[9] + " " + row[10]);
            requestedBy.setUsername((String) row[11]);
            requestedBy.setProfilePictureUrl((String) row[12]);
            requestedBy.setVerified(toBool(row[13]));
            requestedBy.setCreatedAt((Date) row[14]);
            response.setRequestedBy(requestedBy);

            int reviewerId = ((Number) row[15]).intValue();
            if (reviewerId != 0) {
                UserResponse reviewedBy = new UserResponse();
                reviewedBy.setId(reviewerId);
                reviewedBy.setFullName(row[16] + " " + row[17]);
                reviewedBy.setUsername((String) row[18]);
                reviewedBy.setProfilePictureUrl((String) row[19]);
                reviewedBy.setVerified(toBool(row[20]));
                reviewedBy.setCreatedAt((Date) row[21]);
                response.setReviewedBy(reviewedBy);
            } else {
                response.setReviewedBy(null);
            }

            results.add(response);
        }
        return results;
    }

    /**
     * MySQL Connector/J can return boolean-like columns in three different Java types:
     *   Boolean  — TINYINT(1) when tinyInt1isBit=true (default)
     *   byte[]   — BIT(1) columns (always returned as raw bytes)
     *   Number   — TINYINT(1) when tinyInt1isBit=false, or IFNULL(col, 0) integer fallback
     */
    private boolean toBool(Object val) {
        if (val instanceof Boolean b) return b;
        if (val instanceof byte[] bytes) return bytes.length > 0 && bytes[0] != 0;
        return ((Number) val).intValue() != 0;
    }
}
