package net.fosterlink.fosterlinkbackend.mail.service;

import net.fosterlink.fosterlinkbackend.mail.CheckEmailPreference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Mail service for user-related operations (registration, login, profile, etc.).
 * Uses external HTML templates under templates/mail/.
 */
@Service
public class UserMailService {

    private static final String REGISTRATION_TEMPLATE = "mail/registration-thank-you";
    private static final String EMAIL_VERIFICATION_TEMPLATE = "mail/email-verification";

    private final MailSendHelper mailSendHelper;

    public UserMailService(MailSendHelper mailSendHelper) {
        this.mailSendHelper = mailSendHelper;
    }

    /**
     * Sends an email verification link to the given address. Does not throw; failures are logged.
     * Skipped if the user has opted out via {@link CheckEmailPreference}.
     */
    @CheckEmailPreference("EMAIL_VERIFICATION")
    @Async
    public void sendVerificationEmail(int userId, String toEmail, String firstName, String verifyToken, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("verifyUrl", mailSendHelper.buildVerifyEmailUrl(userId, verifyToken));
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "Verify your email - FosterLink", EMAIL_VERIFICATION_TEMPLATE, context);
    }

    /**
     * Sends a thank-you email to the given address after registration.
     * Does not throw; failures are logged and ignored so registration is not affected.
     * Skipped if the user has opted out via {@link CheckEmailPreference}.
     */
    @CheckEmailPreference("REGISTRATION_THANK_YOU")
    @Async
    public void sendThankYouForRegistering(int userId, String toEmail, String firstName, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));

        mailSendHelper.sendTemplatedEmail(toEmail, "Thank you for registering with FosterLink", REGISTRATION_TEMPLATE, context);
    }
}
