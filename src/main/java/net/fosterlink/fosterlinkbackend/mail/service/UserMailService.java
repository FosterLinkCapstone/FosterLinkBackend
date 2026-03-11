package net.fosterlink.fosterlinkbackend.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import net.fosterlink.fosterlinkbackend.mail.CheckEmailPreference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Mail service for user-related operations (registration, login, profile, etc.).
 * Uses external HTML templates under templates/mail/.
 */
@Service
public class UserMailService {

    private static final String REGISTRATION_TEMPLATE = "mail/registration-thank-you";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    public UserMailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends a thank-you email to the given address after registration.
     * Does not throw; failures are logged and ignored so registration is not affected.
     * Skipped if the user has opted out via {@link CheckEmailPreference}.
     */
    @CheckEmailPreference("REGISTRATION_THANK_YOU")
    public void sendThankYouForRegistering(int userId, String toEmail, String firstName) {
        try {
            String greetingName = (firstName != null && !firstName.isBlank()) ? firstName : "there";
            Context context = new Context(Locale.getDefault());
            context.setVariable("greetingName", greetingName);
            context.setVariable("unsubscribeUrl", frontendUrl + "/settings");

            String htmlBody = templateEngine.process(REGISTRATION_TEMPLATE, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Thank you for registering with FosterLink");
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send registration thank-you email to " + toEmail + ": " + e.getMessage());
        }
    }
}
