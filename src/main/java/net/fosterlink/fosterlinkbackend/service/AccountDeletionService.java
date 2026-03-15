package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.*;
import net.fosterlink.fosterlinkbackend.mail.service.AccountDeletionMailService;
import net.fosterlink.fosterlinkbackend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.CacheManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AccountDeletionService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.emailHashPepper}")
    private String emailHashPepper;

    @Autowired private AccountDeletionMailService accountDeletionMailService;
    @Autowired private TokenAuthService tokenAuthService;
    @Autowired private AccountDeletionRequestRepository deletionRequestRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private MailingListMemberRepository mailingListMemberRepository;
    @Autowired private DontSendEmailRepository dontSendEmailRepository;
    @Autowired private ThreadRepository threadRepository;
    @Autowired private ThreadReplyRepository threadReplyRepository;
    @Autowired private ThreadLikeRepository threadLikeRepository;
    @Autowired private ThreadReplyLikeRepository threadReplyLikeRepository;
    @Autowired private ThreadTagRepository threadTagRepository;
    @Autowired private FAQRepository faqRepository;
    @Autowired private FAQApprovalRepository faqApprovalRepository;
    @Autowired private FAQRequestRepository faqRequestRepository;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private AgencyDeletionRequestRepository agencyDeletionRequestRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CacheManager cacheManager;
    @Autowired private BanStatusService banStatusService;

    /** Creates an account deletion request for the given user. Locks the account and hides all content immediately. */
    @Transactional
    public AccountDeletionRequestEntity requestDeletion(UserEntity user, boolean clearAccount) {
        AccountDeletionRequestEntity request = new AccountDeletionRequestEntity();
        request.setRequestedBy(user);
        request.setRequestedAt(new Date());
        request.setAutoApproveBy(thirtyDaysFromNow());
        request.setClearAccount(clearAccount);
        request.setApproved(false);
        request.setAutoApproved(false);
        request.setRequestedByEmailHash(hashEmail(user.getEmail()));

        AccountDeletionRequestEntity saved = deletionRequestRepository.save(request);

        user.setAccountDeleted(true);
        userRepository.save(user);

        hideUserContent(user);

        // Evict profile metadata so the deletion state is reflected immediately
        banStatusService.evictProfileMetadata(user.getId());

        accountDeletionMailService.sendDeletionRequestedConfirmation(
                user.getId(), user.getEmail(), user.getFirstName(), saved.getAutoApproveBy());

        // H-3: resolve all admin unsubscribe tokens in at most two batch operations
        List<UserEntity> admins = userRepository.findAllAdministrators();
        Map<Integer, String> adminTokens = tokenAuthService.getOrCreateUnsubscribeTokens(admins);
        for (UserEntity admin : admins) {
            accountDeletionMailService.sendDeletionRequestAdminNotice(
                    admin.getId(), admin.getEmail(), admin.getFirstName(),
                    user.getUsername(), saved.getAutoApproveBy(), adminTokens.get(admin.getId()));
        }

        return saved;
    }

    /** Cancels a pending deletion request. Only the account owner may cancel. Unlocks the account and restores hidden content. */
    @Transactional
    public void cancelDeletion(AccountDeletionRequestEntity request) {
        UserEntity user = request.getRequestedBy();
        deletionRequestRepository.deleteById(request.getId());

        user.setAccountDeleted(false);
        userRepository.save(user);

        unhideUserContent(user.getId());
        agencyDeletionRequestRepository.deletePendingByAgencyAgentId(user.getId());

        banStatusService.evictProfileMetadata(user.getId());

        accountDeletionMailService.sendDeletionCancelledConfirmation(user.getId(), user.getEmail(), user.getFirstName());
    }

    /** Admin approves a deletion request. Marks it as approved, then executes the deletion. */
    @Transactional
    public void approveDeletion(AccountDeletionRequestEntity request, UserEntity admin) {
        UserEntity user = request.getRequestedBy();
        String email = user.getEmail();
        String firstName = user.getFirstName();

        request.setApproved(true);
        request.setReviewedAt(new Date());
        request.setReviewedBy(admin);
        deletionRequestRepository.save(request);
        executeAccountDeletion(request);

        accountDeletionMailService.sendDeletionApprovedNotification(user.getId(), email, firstName);
    }

    /**
     * Admin records a delay note on a deletion request. Does not change the scheduled deletion
     * time (autoApproveBy); only updates the delay reason and reviewer. The scheduled time
     * remains as originally set or from a previous delay, subject to the 30-day-from-request cap.
     */
    @Transactional
    public void delayDeletion(AccountDeletionRequestEntity request, UserEntity admin, String reason) {
        request.setDelayNote(reason);
        request.setReviewedBy(admin);
        deletionRequestRepository.save(request);

        UserEntity user = request.getRequestedBy();
        accountDeletionMailService.sendDeletionDelayedNotification(
                user.getId(), user.getEmail(), user.getFirstName(), request.getAutoApproveBy(), reason);
    }

    /**
     * Finds all requests past their auto-approve date and executes the deletion for each.
     * Called by the scheduled job every hour.
     */
    @Transactional
    public void processAutoApprovals() {
        List<AccountDeletionRequestEntity> expired = deletionRequestRepository.findAllPastAutoApproveDate();
        for (AccountDeletionRequestEntity request : expired) {
            UserEntity user = request.getRequestedBy();
            String email = user.getEmail();
            String firstName = user.getFirstName();

            request.setAutoApproved(true);
            request.setApproved(true);
            request.setReviewedAt(new Date());
            deletionRequestRepository.save(request);
            executeAccountDeletion(request);

            accountDeletionMailService.sendDeletionApprovedNotification(user.getId(), email, firstName);
        }
    }

    /**
     * Sends 7-day auto-approval warnings for pending deletion requests approaching their deadline.
     * Called by the scheduler independently of processAutoApprovals.
     */
    @Transactional(readOnly = true)
    public void processAutoApprovalWarnings() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        Date sevenDaysFromNow = cal.getTime();

        List<AccountDeletionRequestEntity> approaching =
                deletionRequestRepository.findApproachingAutoApproval(now, sevenDaysFromNow);
        for (AccountDeletionRequestEntity request : approaching) {
            UserEntity user = request.getRequestedBy();
            accountDeletionMailService.sendAutoApprovalWarning(
                    user.getId(), user.getEmail(), user.getFirstName(), request.getAutoApproveBy());
        }
    }

    /**
     * Anonymizes the user record and optionally clears all associated content.
     * If not clearing, hidden content is restored so it remains public under the anonymized account.
     * After this method, the user row is retained but contains no identifying information.
     */
    @Transactional
    public void executeAccountDeletion(AccountDeletionRequestEntity request) {
        UserEntity user = request.getRequestedBy();
        int userId = user.getId();

        if (request.isClearAccount()) {
            clearAccountContent(userId);
        } else {
            unhideUserContent(userId);
        }

        anonymizeUser(user);
    }

    /**
     * Hides all visible content belonging to the user:
     *   - Threads/replies via the existing user-delete hide flag
     *   - Approved FAQs via the hidden-by-author flag
     *   - Agencies by creating a pending agency deletion request for each agency
     *     that does not already have one (so they appear in the admin deletion queue)
     */
    private void hideUserContent(UserEntity user) {
        int userId = user.getId();
        threadRepository.hideVisibleThreadsByUserId(userId);
        threadReplyRepository.hideVisibleRepliesByUserId(userId);
        faqApprovalRepository.hideApprovedFaqsByAuthorId(userId, user.getUsername());

        // M-1: resolve which agencies already have a pending request in one query instead of N
        List<AgencyEntity> userAgencies = agencyRepository.findByAgentId(userId);
        if (!userAgencies.isEmpty()) {
            List<Integer> agencyIds = userAgencies.stream().map(AgencyEntity::getId).toList();
            Set<Integer> agenciesWithPending = new HashSet<>(
                    agencyDeletionRequestRepository.findPendingAgencyIds(agencyIds));
            for (AgencyEntity agency : userAgencies) {
                if (!agenciesWithPending.contains(agency.getId())) {
                    AgencyDeletionRequestEntity adr = new AgencyDeletionRequestEntity();
                    adr.setAgency(agency);
                    adr.setRequestedBy(user);
                    adr.setCreatedAt(new Date());
                    adr.setAutoApproveBy(AgencyDeletionService.thirtyDaysFromNow());
                    agencyDeletionRequestRepository.save(adr);
                }
            }
        }

        cacheManager.getCache("faqApprovedPreviews").invalidate();
    }

    /**
     * Reverses hideUserContent for threads, replies, and FAQs — restores only content
     * hidden via the deletion request flow. Agency deletion requests are handled separately
     * by the caller (cancel deletes them; approval leaves them for admin processing).
     */
    private void unhideUserContent(int userId) {
        threadRepository.unhideUserHiddenThreadsByUserId(userId);
        threadReplyRepository.unhideUserHiddenRepliesByUserId(userId);
        faqApprovalRepository.unhideAuthorHiddenFaqsByAuthorId(userId);
        cacheManager.getCache("faqApprovedPreviews").invalidate();
    }

    /** Deletes all content associated with a user: threads, replies, likes, FAQs, agencies, and requests. */
    private void clearAccountContent(int userId) {
        // Delete all likes placed by the user (on others' threads and replies)
        threadLikeRepository.deleteByUserId(userId);
        threadReplyLikeRepository.deleteByUserId(userId);

        // Delete threads by user (including all their replies, likes, tags)
        List<Integer> userThreadIds = threadRepository.findIdsByPostedById(userId);
        if (!userThreadIds.isEmpty()) {
            // H-1: fetch all replies on user's threads in one query instead of N
            List<ThreadReplyEntity> repliesOnUserThreads =
                    threadReplyRepository.findByThreadIdIn(userThreadIds);
            List<Integer> replyIdsOnUserThreads = repliesOnUserThreads.stream()
                    .map(ThreadReplyEntity::getId).toList();
            if (!replyIdsOnUserThreads.isEmpty()) {
                threadReplyLikeRepository.deleteByThreadIn(replyIdsOnUserThreads);
            }
            // Delete replies to user's threads (JPA cascade handles post_metadata)
            threadReplyRepository.deleteAll(repliesOnUserThreads);
            // Delete thread likes and tags on user's threads
            threadLikeRepository.deleteByThreadIdIn(userThreadIds);
            threadTagRepository.deleteByThreadIdIn(userThreadIds);
            // Delete threads themselves (JPA cascade handles post_metadata)
            List<ThreadEntity> userThreads = threadRepository.findAllByPostedById(userId);
            threadRepository.deleteAll(userThreads);
        }

        // Delete replies by user on other threads
        List<ThreadReplyEntity> userRepliesOnOtherThreads = threadReplyRepository.findAllByPostedById(userId);
        if (!userRepliesOnOtherThreads.isEmpty()) {
            List<Integer> userReplyIds = userRepliesOnOtherThreads.stream()
                    .map(ThreadReplyEntity::getId).toList();
            threadReplyLikeRepository.deleteByThreadIn(userReplyIds);
            threadReplyRepository.deleteAll(userRepliesOnOtherThreads);
        }

        // Delete FAQ approvals and FAQs by user
        faqApprovalRepository.deleteByFaqAuthorId(userId);
        faqRepository.deleteByAuthorId(userId);

        // Delete FAQ requests by user
        faqRequestRepository.deleteByRequestedById(userId);

        // Delete agency deletion requests and agencies by user
        agencyDeletionRequestRepository.deleteByAgencyAgentId(userId);
        List<AgencyEntity> userAgencies = agencyRepository.findByAgentId(userId);
        if (!userAgencies.isEmpty()) {
            // L-1: collect non-null address IDs then batch-delete locations
            List<Integer> addressIds = userAgencies.stream()
                    .map(AgencyEntity::getAddress)
                    .filter(a -> a != null)
                    .map(LocationEntity::getId)
                    .toList();
            for (AgencyEntity agency : userAgencies) {
                agencyRepository.deleteAgencyById(agency.getId());
            }
            if (!addressIds.isEmpty()) {
                locationRepository.deleteAllByIds(addressIds);
            }
        }

        // Delete the account deletion request history itself
        deletionRequestRepository.deleteByUserId(userId);

        cacheManager.getCache("faqApprovedPreviews").invalidate();
        cacheManager.getCache("agencyApprovedRows").invalidate();
    }

    private void anonymizeUser(UserEntity user) {
        user.setFirstName("Deleted");
        user.setLastName("Account");
        user.setUsername("deleted_account_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        user.setEmail(hashEmail(user.getEmail()));
        user.setPhoneNumber(null);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setProfilePictureUrl(null);
        user.setIdVerified(false);
        user.setVerifiedFoster(false);
        user.setVerifiedAgencyRep(false);
        user.setAdministrator(false);
        user.setFaqAuthor(false);
        user.setEmailVerified(false);
        user.setAccountDeleted(true);
        // Revoke all active sessions so the anonymized account cannot be accessed
        refreshTokenRepository.deleteAllByUserId(user.getId());
        // Remove mailing list memberships so no future emails are sent to the hashed address
        mailingListMemberRepository.deleteAllByUserId(user.getId());
        // Remove opt-out rows so they do not persist against the anonymized account
        dontSendEmailRepository.deleteAllByUserId(user.getId());
        // Clear the unsubscribe token so no live email link can identify this user
        user.setUnsubscribeToken(null);
        userRepository.save(user);

        banStatusService.evictProfileMetadata(user.getId());
    }

    private String hashEmail(String email) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    emailHashPepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] digest = mac.doFinal(email.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to hash email for account deletion", e);
        }
    }

    private Date thirtyDaysFromNow() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 30);
        return cal.getTime();
    }

    private Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
