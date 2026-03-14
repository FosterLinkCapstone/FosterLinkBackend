package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.fosterlink.fosterlinkbackend.entities.AccountDeletionRequestEntity;
import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.ConsentRecordEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;

import java.util.Date;
import java.util.List;

/**
 * Aggregated personal data export for GDPR right of access (RIGHTS-01) and
 * right to data portability (RIGHTS-04). Excludes password hash, auth token
 * version, and raw unsubscribe token.
 */
@Data
@Schema(description = "All personal data held for the currently authenticated user.")
public class UserDataExportResponse {

    // --- Profile -----------------------------------------------------------------

    @Schema(description = "Internal user ID")
    private int id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private boolean idVerified;
    private boolean verifiedFoster;
    private boolean verifiedAgencyRep;
    private boolean administrator;
    private boolean faqAuthor;
    private boolean emailVerified;
    private boolean accountDeleted;
    private boolean unsubscribeAll;
    private Date createdAt;
    private Date updatedAt;
    private Date bannedAt;
    private Date restrictedAt;
    private Date restrictedUntil;

    // --- Content -----------------------------------------------------------------

    @Schema(description = "Forum threads authored by this user")
    private List<ThreadSummary> threads;

    @Schema(description = "Agencies created/represented by this user")
    private List<AgencySummary> agencies;

    // --- Communication preferences -----------------------------------------------

    @Schema(description = "Email type IDs for which the user has disabled notifications")
    private List<Integer> disabledEmailTypeIds;

    @Schema(description = "Mailing list IDs the user belongs to")
    private List<Integer> mailingListIds;

    // --- Account state -----------------------------------------------------------

    @Schema(description = "Pending account deletion request, or null if none exists")
    private AccountDeletionSummary accountDeletionRequest;

    @Schema(description = "Consent records for this user")
    private List<ConsentSummary> consentRecords;

    // ---- Static inner summaries -------------------------------------------------

    @Data
    @Schema(description = "Brief summary of a thread authored by the user")
    public static class ThreadSummary {
        public ThreadSummary(ThreadEntity t) {
            this.id = t.getId();
            this.title = t.getTitle();
            this.content = t.getContent();
            this.createdAt = t.getCreatedAt();
            this.updatedAt = t.getUpdatedAt();
            this.hidden = t.getPostMetadata() != null && t.getPostMetadata().isHidden();
            this.userDeleted = t.getPostMetadata() != null && t.getPostMetadata().isUser_deleted();
        }

        private int id;
        private String title;
        private String content;
        private Date createdAt;
        private Date updatedAt;
        private boolean hidden;
        private boolean userDeleted;
    }

    @Data
    @Schema(description = "Brief summary of an agency associated with the user")
    public static class AgencySummary {
        public AgencySummary(AgencyEntity a) {
            this.id = a.getId();
            this.name = a.getName();
            this.websiteUrl = a.getWebsiteUrl();
            this.missionStatement = a.getMissionStatement();
            this.approved = a.getApproved();
            this.hidden = a.isHidden();
            this.createdAt = a.getCreatedAt();
            this.updatedAt = a.getUpdatedAt();
        }

        private int id;
        private String name;
        private String websiteUrl;
        private String missionStatement;
        private Boolean approved;
        private boolean hidden;
        private Date createdAt;
        private Date updatedAt;
    }

    @Data
    @Schema(description = "Pending account deletion request summary")
    public static class AccountDeletionSummary {
        public AccountDeletionSummary(AccountDeletionRequestEntity dr) {
            this.requestId = dr.getId();
            this.requestedAt = dr.getRequestedAt();
            this.autoApproveBy = dr.getAutoApproveBy();
            this.clearAccount = dr.isClearAccount();
        }

        private int requestId;
        private Date requestedAt;
        private Date autoApproveBy;
        private boolean clearAccount;
    }

    @Data
    @Schema(description = "A single consent record entry")
    public static class ConsentSummary {
        public ConsentSummary(ConsentRecordEntity c) {
            this.consentType = c.getConsentType();
            this.granted = c.isGranted();
            this.timestamp = c.getTimestamp().toString();
            this.policyVersion = c.getPolicyVersion();
            this.mechanism = c.getMechanism();
        }

        private String consentType;
        private boolean granted;
        private String timestamp;
        private String policyVersion;
        private String mechanism;
    }

    // --- Factory -----------------------------------------------------------------

    public static UserDataExportResponse from(
            UserEntity user,
            List<ThreadEntity> threads,
            List<AgencyEntity> agencies,
            List<Integer> disabledEmailTypeIds,
            List<Integer> mailingListIds,
            AccountDeletionRequestEntity deletionRequest,
            List<ConsentRecordEntity> consentRecords) {

        UserDataExportResponse r = new UserDataExportResponse();
        r.id = user.getId();
        r.firstName = user.getFirstName();
        r.lastName = user.getLastName();
        r.username = user.getUsername();
        r.email = user.getEmail();
        r.phoneNumber = user.getPhoneNumber();
        r.profilePictureUrl = user.getProfilePictureUrl();
        r.idVerified = user.isIdVerified();
        r.verifiedFoster = user.isVerifiedFoster();
        r.verifiedAgencyRep = user.isVerifiedAgencyRep();
        r.administrator = user.isAdministrator();
        r.faqAuthor = user.isFaqAuthor();
        r.emailVerified = user.isEmailVerified();
        r.accountDeleted = user.isAccountDeleted();
        r.unsubscribeAll = user.isUnsubscribeAll();
        r.createdAt = user.getCreatedAt();
        r.updatedAt = user.getUpdatedAt();
        r.bannedAt = user.getBannedAt();
        r.restrictedAt = user.getRestrictedAt();
        r.restrictedUntil = user.getRestrictedUntil();

        r.threads = threads.stream().map(ThreadSummary::new).toList();
        r.agencies = agencies.stream().map(AgencySummary::new).toList();
        r.disabledEmailTypeIds = disabledEmailTypeIds;
        r.mailingListIds = mailingListIds;
        r.accountDeletionRequest = deletionRequest != null ? new AccountDeletionSummary(deletionRequest) : null;
        r.consentRecords = consentRecords.stream().map(ConsentSummary::new).toList();

        return r;
    }
}
