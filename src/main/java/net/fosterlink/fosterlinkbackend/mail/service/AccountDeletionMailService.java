package net.fosterlink.fosterlinkbackend.mail.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.Locale;

/**
 * Mail service for account deletion operations.
 * None of these emails are gated by @CheckEmailPreference — they are security-critical
 * notifications that must always be delivered regardless of marketing opt-out preferences.
 */
@Service
public class AccountDeletionMailService {

    private static final String DELETION_REQUESTED_TEMPLATE = "mail/account-deletion-requested";
    private static final String DELETION_APPROVED_TEMPLATE = "mail/account-deletion-approved";
    private static final String DELETION_DELAYED_TEMPLATE = "mail/account-deletion-delayed";
    private static final String DELETION_WARNING_TEMPLATE = "mail/account-deletion-warning";
    private static final String DELETION_CANCELLED_TEMPLATE = "mail/account-deletion-cancelled";
    private static final String DELETION_ADMIN_NOTICE_TEMPLATE = "mail/account-deletion-admin-notice";

    private final MailSendHelper mailSendHelper;

    public AccountDeletionMailService(MailSendHelper mailSendHelper) {
        this.mailSendHelper = mailSendHelper;
    }

    /**
     * Sent to the user immediately after they submit an account deletion request.
     * Informs them the request was received and when it will auto-approve.
     */
    @Async
    public void sendDeletionRequestedConfirmation(int userId, String toEmail, String firstName, Date autoApproveBy) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("autoApproveBy", autoApproveBy);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, null));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your account deletion request has been received - FosterLink", DELETION_REQUESTED_TEMPLATE, context);
    }

    /**
     * Sent to the user after their account has been approved for deletion and anonymized.
     * Call this BEFORE anonymizeUser() so that the real email address is still available.
     * No unsubscribe link — the account no longer exists.
     */
    @Async
    public void sendDeletionApprovedNotification(String toEmail, String firstName) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your FosterLink account has been deleted", DELETION_APPROVED_TEMPLATE, context);
    }

    /**
     * Sent to the user when an administrator delays their deletion request by 30 days.
     * Includes the admin's reason and the new auto-approval date.
     */
    @Async
    public void sendDeletionDelayedNotification(int userId, String toEmail, String firstName, Date newAutoApproveBy, String reason) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("newAutoApproveBy", newAutoApproveBy);
        context.setVariable("reason", reason);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, null));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your account deletion has been delayed - FosterLink", DELETION_DELAYED_TEMPLATE, context);
    }

    /**
     * Sent to the user approximately 7 days before their account deletion auto-approves.
     * Gives them a chance to cancel before the deadline.
     */
    @Async
    public void sendAutoApprovalWarning(int userId, String toEmail, String firstName, Date autoApproveBy) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("autoApproveBy", autoApproveBy);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, null));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your FosterLink account is scheduled for deletion in 7 days", DELETION_WARNING_TEMPLATE, context);
    }

    /**
     * Sent to the user after they cancel their pending account deletion request.
     * Confirms their account is restored and active.
     */
    @Async
    public void sendDeletionCancelledConfirmation(int userId, String toEmail, String firstName) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, null));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your account deletion request has been cancelled - FosterLink", DELETION_CANCELLED_TEMPLATE, context);
    }

    /**
     * Sent to each administrator when a new account deletion request is submitted.
     * Lets admins know there is a pending request they may want to review.
     */
    @Async
    public void sendDeletionRequestAdminNotice(int adminId, String toEmail, String adminFirstName,
                                                String requestingUsername, Date autoApproveBy, String adminUnsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(adminFirstName));
        context.setVariable("requestingUsername", requestingUsername);
        context.setVariable("autoApproveBy", autoApproveBy);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(adminId, adminUnsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "New account deletion request submitted - FosterLink", DELETION_ADMIN_NOTICE_TEMPLATE, context);
    }
}
