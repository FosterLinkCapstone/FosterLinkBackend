package net.fosterlink.fosterlinkbackend.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends a thank-you email to newly registered users.
 */
@Service
public class RegistrationMailService {

    /*private final JavaMailSender mailSender; TODO: uncomment once email server is configured

    @Value("${spring.mail.username:noreply@fosterlink.net}")
    private String fromAddress;

    @Value("${app.mail.registration.enabled:true}")
    private boolean enabled;

    public RegistrationMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a thank-you email to the given address after registration.
     * Does not throw; failures are logged and ignored so registration is not affected.
     */
    /*
    public void sendThankYouForRegistering(String toEmail, String firstName) {
        if (!enabled) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Thank you for registering with FosterLink");
            message.setText(
                    "Hi " + (firstName != null && !firstName.isBlank() ? firstName : "there") + ",\n\n"
                    + "Thank you for registering with FosterLink. We're glad to have you.\n\n"
                    + "If you have any questions, feel free to reach out.\n\n"
                    + "Best regards,\nThe FosterLink Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            // Log but do not fail registration
            // TODO: use a proper logger
            System.err.println("Failed to send registration thank-you email to " + toEmail + ": " + e.getMessage());
        }
    }*/
}
