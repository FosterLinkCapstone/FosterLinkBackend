package net.fosterlink.fosterlinkbackend.mail.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.Locale;

/**
 * Mail service for account moderation operations (bans and restrictions).
 * None of these emails are gated by @CheckEmailPreference — they are security-critical
 * notifications that must always be delivered regardless of marketing opt-out preferences.
 * Uses external HTML templates under templates/mail/.
 */
@Service
public class HomeMailService {

    private static final String ACCOUNT_BANNED_TEMPLATE = "mail/account-banned";
    private static final String ACCOUNT_UNBANNED_TEMPLATE = "mail/account-unbanned";
    private static final String ACCOUNT_RESTRICTED_TEMPLATE = "mail/account-restricted";
    private static final String ACCOUNT_UNRESTRICTED_TEMPLATE = "mail/account-unrestricted";

    private final MailSendHelper mailSendHelper;

    public HomeMailService(MailSendHelper mailSendHelper) {
        this.mailSendHelper = mailSendHelper;
    }

    /** Sent to the user when an administrator bans their account. */
    @Async
    public void sendAccountBannedNotification(int userId, String toEmail, String firstName) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        mailSendHelper.sendTemplatedEmail(userId, toEmail, "Your FosterLink account has been suspended", ACCOUNT_BANNED_TEMPLATE, context);
    }

    /** Sent to the user when an administrator lifts the ban on their account. */
    @Async
    public void sendAccountUnbannedNotification(int userId, String toEmail, String firstName, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(userId, toEmail, "Your FosterLink account suspension has been lifted", ACCOUNT_UNBANNED_TEMPLATE, context);
    }

    /**
     * Sent to the user when an administrator restricts their account.
     * If restrictedUntil is null the restriction is indefinite.
     */
    @Async
    public void sendAccountRestrictedNotification(int userId, String toEmail, String firstName,
                                                   Date restrictedUntil, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("restrictedUntil", restrictedUntil);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(userId, toEmail, "Your FosterLink account has been restricted", ACCOUNT_RESTRICTED_TEMPLATE, context);
    }

    /** Sent to the user when an administrator removes the restriction on their account. */
    @Async
    public void sendAccountUnrestrictedNotification(int userId, String toEmail, String firstName, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(userId, toEmail, "Your FosterLink account restriction has been lifted", ACCOUNT_UNRESTRICTED_TEMPLATE, context);
    }
}
