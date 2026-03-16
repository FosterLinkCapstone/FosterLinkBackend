package net.fosterlink.fosterlinkbackend.mail.service;

import jakarta.mail.internet.MimeMessage;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Shared logic for mail services: unsubscribe URL, greeting name, and sending
 * HTML/templated emails. Use this to avoid duplication across AdminUserMailService,
 * UserMailService, etc.
 */
@Component
public class MailSendHelper {

    private static final Logger log = LoggerFactory.getLogger(MailSendHelper.class);

    // GAP-02: tokens are delivered via URL fragments (#) rather than query parameters (?).
    // Fragments are never transmitted to the server in HTTP request logs or CDN logs,
    // so the token is not visible to server-side observers even if the Referer header leaks.
    private static final String UNSUBSCRIBE_ACTION = "/token-action#action=unsubscribe&token=%s&userId=%d";
    private static final String VERIFY_EMAIL_ACTION = "/token-action#action=verify-email&token=%s&userId=%d";
    private static final String RESET_PASSWORD_ACTION = "/reset-password#token=%s&userId=%d";
    private static final String SETTINGS_PATH = "/settings";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * -- GETTER --
     * Exposed for templates that need to build other token-action URLs (e.g. approve/deny).
     */
    @Getter
    @Value("${app.frontendUrl}")
    private String frontendUrl;

    public MailSendHelper(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Builds the unsubscribe URL for a recipient. If token is null, returns the
     * frontend settings page URL so the user can manage preferences there.
     */
    public String buildUnsubscribeUrl(int userId, String unsubscribeToken) {
        if (unsubscribeToken != null && !unsubscribeToken.isBlank()) {
            return frontendUrl + String.format(UNSUBSCRIBE_ACTION, unsubscribeToken, userId);
        }
        return frontendUrl + SETTINGS_PATH;
    }

    /**
     * Builds the email verification URL for a user. Used in verification emails.
     */
    public String buildVerifyEmailUrl(int userId, String verifyToken) {
        return frontendUrl + String.format(VERIFY_EMAIL_ACTION, verifyToken, userId);
    }

    /**
     * Builds the password reset URL for a user. Used in password reset emails.
     */
    public String buildPasswordResetUrl(int userId, String resetToken) {
        return frontendUrl + String.format(RESET_PASSWORD_ACTION, resetToken, userId);
    }

    /**
     * Returns a greeting name for the email body: the given name if non-blank, otherwise "there".
     */
    public String greetingName(String firstName) {
        return (firstName != null && !firstName.isBlank()) ? firstName : "there";
    }

    /**
     * Processes the Thymeleaf template with the given context and sends an HTML email.
     * Failures are logged and not rethrown so callers can ignore send failures.
     *
     * @param toEmail       recipient address
     * @param subject       email subject
     * @param templateName  Thymeleaf template name (e.g. "mail/registration-thank-you")
     * @param context       template context (will use default locale if not set)
     * @return true if sent successfully, false otherwise
     */
    public boolean sendTemplatedEmail(int userId, String toEmail, String subject, String templateName, Context context) {
        try {
            if (context.getLocale() == null) {
                context.setLocale(Locale.getDefault());
            }
            String htmlBody = templateEngine.process(templateName, context);
            sendHtmlEmail(toEmail, subject, htmlBody);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email for user ID {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Sends a pre-rendered HTML email. Used by {@link #sendTemplatedEmail} or when
     * the body is built elsewhere.
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

}
