package net.fosterlink.fosterlinkbackend.mail.service;

import jakarta.mail.internet.MimeMessage;
import net.fosterlink.fosterlinkbackend.mail.CheckEmailPreference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
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

    private static final Map<String, String> ROLE_DISPLAY_NAMES = Map.of(
            "FAQ_AUTHOR", "FAQ Author",
            "VERIFIED_FOSTER", "Verified Foster",
            "AGENCY_REP", "Agency Representative",
            "ID_VERIFIED", "ID Verified",
            "ADMINISTRATOR", "Administrator"
    );

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    public AdminUserMailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendAdminApprovalRequest(String toEmail, String founderFirstName, String targetUsername,
                                         int targetUserId, String rawToken, String frontendUrl) {
        try {
            String greeting = (founderFirstName != null && !founderFirstName.isBlank()) ? founderFirstName : "there";
            String approveUrl = frontendUrl + "/token-action?action=approve&token=" + rawToken + "&userId=" + targetUserId;
            String denyUrl    = frontendUrl + "/token-action?action=deny&token="    + rawToken + "&userId=" + targetUserId;
            String unsubscribeUrl = frontendUrl + "/settings";

            Context context = new Context(Locale.getDefault());
            context.setVariable("founderName", greeting);
            context.setVariable("targetUsername", targetUsername);
            context.setVariable("approveUrl", approveUrl);
            context.setVariable("denyUrl", denyUrl);
            context.setVariable("unsubscribeUrl", unsubscribeUrl);

            String htmlBody = templateEngine.process(ADMIN_APPROVAL_TEMPLATE, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Action required: Administrator role assignment approval");
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send admin approval email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendAdminRevocationRequest(String toEmail, String founderFirstName, String targetUsername,
                                           int targetUserId, String rawToken, String frontendUrl) {
        try {
            String greeting = (founderFirstName != null && !founderFirstName.isBlank()) ? founderFirstName : "there";
            String approveUrl = frontendUrl + "/token-action?action=approve-revoke&token=" + rawToken + "&userId=" + targetUserId;
            String denyUrl    = frontendUrl + "/token-action?action=deny-revoke&token="    + rawToken + "&userId=" + targetUserId;
            String unsubscribeUrl = frontendUrl + "/settings";

            Context context = new Context(Locale.getDefault());
            context.setVariable("founderName", greeting);
            context.setVariable("targetUsername", targetUsername);
            context.setVariable("approveUrl", approveUrl);
            context.setVariable("denyUrl", denyUrl);
            context.setVariable("unsubscribeUrl", unsubscribeUrl);

            String htmlBody = templateEngine.process(ADMIN_REVOCATION_TEMPLATE, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Action required: Administrator role revocation approval");
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send admin revocation email to " + toEmail + ": " + e.getMessage());
        }
    }

    @CheckEmailPreference(value = "ROLE_ASSIGNED", uiName = "New role assigned (e.g. FAQ author or verified)")
    @Async
    public void sendRoleAssignedNotification(int userId, String toEmail, String firstName, String role) {
        try {
            String greetingName = (firstName != null && !firstName.isBlank()) ? firstName : "there";
            String roleDisplayName = ROLE_DISPLAY_NAMES.getOrDefault(role, role);

            Context context = new Context(Locale.getDefault());
            context.setVariable("greetingName", greetingName);
            context.setVariable("roleDisplayName", roleDisplayName);
            context.setVariable("unsubscribeUrl", frontendUrl + "/settings");

            String htmlBody = templateEngine.process(ROLE_ASSIGNED_TEMPLATE, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("You've been assigned a new role on FosterLink");
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send role-assignment email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendRoleRevokedNotification(int userId, String toEmail, String firstName, String role) {
        try {
            String greetingName = (firstName != null && !firstName.isBlank()) ? firstName : "there";
            String roleDisplayName = ROLE_DISPLAY_NAMES.getOrDefault(role, role);

            Context context = new Context(Locale.getDefault());
            context.setVariable("greetingName", greetingName);
            context.setVariable("roleDisplayName", roleDisplayName);
            context.setVariable("unsubscribeUrl", frontendUrl + "/settings");

            String htmlBody = templateEngine.process(ROLE_REVOKED_TEMPLATE, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Your role has been updated on FosterLink");
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send role-revocation email to " + toEmail + ": " + e.getMessage());
        }
    }
}
