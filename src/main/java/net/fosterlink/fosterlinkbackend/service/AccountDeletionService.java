package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.*;
import net.fosterlink.fosterlinkbackend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountDeletionService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.emailHashPepper}")
    private String emailHashPepper;

    @Autowired private AccountDeletionRequestRepository deletionRequestRepository;
    @Autowired private UserRepository userRepository;
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
    }

    /** Admin approves a deletion request. Marks it as approved, then executes the deletion. */
    @Transactional
    public void approveDeletion(AccountDeletionRequestEntity request, UserEntity admin) {
        request.setApproved(true);
        request.setReviewedAt(new Date());
        request.setReviewedBy(admin);
        deletionRequestRepository.save(request);
        executeAccountDeletion(request);
    }

    /**
     * Admin delays a deletion request by 30 days. Records the reason and updates the reviewer.
     * Only one active delay note is kept (overwrites the previous).
     */
    @Transactional
    public void delayDeletion(AccountDeletionRequestEntity request, UserEntity admin, String reason) {
        request.setDelayNote(reason);
        request.setAutoApproveBy(thirtyDaysFromNow());
        request.setReviewedBy(admin);
        deletionRequestRepository.save(request);
    }

    /**
     * Finds all requests past their auto-approve date and executes the deletion for each.
     * Called by the scheduled job every hour.
     */
    @Transactional
    public void processAutoApprovals() {
        List<AccountDeletionRequestEntity> expired = deletionRequestRepository.findAllPastAutoApproveDate();
        for (AccountDeletionRequestEntity request : expired) {
            request.setAutoApproved(true);
            request.setApproved(true);
            request.setReviewedAt(new Date());
            deletionRequestRepository.save(request);
            executeAccountDeletion(request);
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

        List<AgencyEntity> userAgencies = agencyRepository.findByAgentId(userId);
        for (AgencyEntity agency : userAgencies) {
            if (agencyDeletionRequestRepository.findPendingByAgencyId(agency.getId()).isEmpty()) {
                AgencyDeletionRequestEntity adr = new AgencyDeletionRequestEntity();
                adr.setAgency(agency);
                adr.setRequestedBy(user);
                adr.setCreatedAt(new Date());
                agencyDeletionRequestRepository.save(adr);
            }
        }
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
    }

    /** Deletes all content associated with a user: threads, replies, likes, FAQs, agencies, and requests. */
    private void clearAccountContent(int userId) {
        // Delete all likes placed by the user (on others' threads and replies)
        threadLikeRepository.deleteByUserId(userId);
        threadReplyLikeRepository.deleteByUserId(userId);

        // Delete threads by user (including all their replies, likes, tags)
        List<Integer> userThreadIds = threadRepository.findIdsByPostedById(userId);
        if (!userThreadIds.isEmpty()) {
            // Delete reply likes on all replies to user's threads
            List<ThreadReplyEntity> repliesOnUserThreads = userThreadIds.stream()
                    .flatMap(tid -> threadReplyRepository.findByThreadId(tid).stream())
                    .toList();
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
        for (AgencyEntity agency : userAgencies) {
            LocationEntity address = agency.getAddress();
            agencyRepository.deleteAgencyById(agency.getId());
            if (address != null) {
                locationRepository.deleteById(address.getId());
            }
        }

        // Delete the account deletion request history itself
        deletionRequestRepository.deleteByUserId(userId);
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
        userRepository.save(user);
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
}
