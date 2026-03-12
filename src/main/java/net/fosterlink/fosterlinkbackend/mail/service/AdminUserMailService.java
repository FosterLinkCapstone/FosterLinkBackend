package net.fosterlink.fosterlinkbackend.mail.service;

import net.fosterlink.fosterlinkbackend.mail.CheckEmailPreference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

/**
 * Mail service for admin user management operations (search, role assignment, etc.).
 * Uses external HTML templates under templates/mail/.
 */
@Service
public class AdminUserMailService {

    private static final String ROLE_ASSIGNED_TEMPLATE = "mail/role-assigned";
    private static final String ROLE_REVOKED_TEMPLATE = "mail/role-revoked";
    private static final String ADMIN_APPROVAL_TEMPLATE = "mail/admin-role-approval";
    private static final String ADMIN_REVOCATION_TEMPLATE = "mail/admin-role-revocation";
    private static final String PROFILE_CLEARED_TEMPLATE = "mail/profile-cleared";

    private static final Map<String, String> ROLE_DISPLAY_NAMES = Map.of(
            "FAQ_AUTHOR", "FAQ Author",
            "VERIFIED_FOSTER", "Verified Foster",
            "AGENCY_REP", "Agency Representative",
            "ID_VERIFIED", "ID Verified",
            "ADMINISTRATOR", "Administrator"
    );

    private final MailSendHelper mailSendHelper;

    public AdminUserMailService(MailSendHelper mailSendHelper) {
        this.mailSendHelper = mailSendHelper;
    }

    @Async
    public void sendAdminApprovalRequest(String toEmail, String founderFirstName, String targetUsername,
                                         int targetUserId, String rawToken, String frontendUrl,
                                         String recipientUnsubscribeToken, int recipientId) {
        String baseUrl = frontendUrl != null ? frontendUrl : mailSendHelper.getFrontendUrl();
        String approveUrl = baseUrl + "/token-action?action=approve&token=" + rawToken + "&userId=" + targetUserId;
        String denyUrl    = baseUrl + "/token-action?action=deny&token="    + rawToken + "&userId=" + targetUserId;

        Context context = new Context(Locale.getDefault());
        context.setVariable("founderName", mailSendHelper.greetingName(founderFirstName));
        context.setVariable("targetUsername", targetUsername);
        context.setVariable("approveUrl", approveUrl);
        context.setVariable("denyUrl", denyUrl);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(recipientId, recipientUnsubscribeToken));

        mailSendHelper.sendTemplatedEmail(toEmail, "Action required: Administrator role assignment approval", ADMIN_APPROVAL_TEMPLATE, context);
    }

    @Async
    public void sendAdminRevocationRequest(String toEmail, String founderFirstName, String targetUsername,
                                           int targetUserId, String rawToken, String frontendUrl,
                                           String recipientUnsubscribeToken, int recipientId) {
        String baseUrl = frontendUrl != null ? frontendUrl : mailSendHelper.getFrontendUrl();
        String approveUrl = baseUrl + "/token-action?action=approve-revoke&token=" + rawToken + "&userId=" + targetUserId;
        String denyUrl    = baseUrl + "/token-action?action=deny-revoke&token="    + rawToken + "&userId=" + targetUserId;

        Context context = new Context(Locale.getDefault());
        context.setVariable("founderName", mailSendHelper.greetingName(founderFirstName));
        context.setVariable("targetUsername", targetUsername);
        context.setVariable("approveUrl", approveUrl);
        context.setVariable("denyUrl", denyUrl);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(recipientId, recipientUnsubscribeToken));

        mailSendHelper.sendTemplatedEmail(toEmail, "Action required: Administrator role revocation approval", ADMIN_REVOCATION_TEMPLATE, context);
    }

    @CheckEmailPreference(value = "ROLE_ASSIGNED", uiName = "New role assigned (e.g. FAQ author or verified)")
    @Async
    public void sendRoleAssignedNotification(int userId, String toEmail, String firstName, String role, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("roleDisplayName", ROLE_DISPLAY_NAMES.getOrDefault(role, role));
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));

        mailSendHelper.sendTemplatedEmail(toEmail, "You've been assigned a new role on FosterLink", ROLE_ASSIGNED_TEMPLATE, context);
    }

    @Async
    public void sendRoleRevokedNotification(int userId, String toEmail, String firstName, String role, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("roleDisplayName", ROLE_DISPLAY_NAMES.getOrDefault(role, role));
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));

        mailSendHelper.sendTemplatedEmail(toEmail, "Your role has been updated on FosterLink", ROLE_REVOKED_TEMPLATE, context);
    }

    @CheckEmailPreference(value = "PROFILE_CLEARED", uiName = "Profile cleared by administrator")
    @Async
    public void sendProfileClearedNotification(int userId, String toEmail, String firstName, String clearedFields, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("clearedFields", clearedFields);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));

        mailSendHelper.sendTemplatedEmail(toEmail, "Your profile has been updated by an administrator", PROFILE_CLEARED_TEMPLATE, context);
    }
}
