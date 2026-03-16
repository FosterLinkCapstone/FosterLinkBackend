package net.fosterlink.fosterlinkbackend.mail.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Mail service for content moderation notifications sent to users when an administrator
 * hides or removes their content.
 * Not gated by @CheckEmailPreference — users should always be informed when their
 * content is hidden by a moderator.
 */
@Service
public class AdminUserContentMailService {

    private static final String CONTENT_MODERATED_TEMPLATE = "mail/content-moderated";

    private final MailSendHelper mailSendHelper;

    public AdminUserContentMailService(MailSendHelper mailSendHelper) {
        this.mailSendHelper = mailSendHelper;
    }

    /**
     * Sent to a user when an administrator hides their thread, reply, or FAQ.
     *
     * @param contentType  human-readable type such as "thread", "reply", or "FAQ"
     * @param contentPreview  a short excerpt of the hidden content (truncated if needed)
     */
    @Async
    public void sendContentModeratedNotification(int userId, String toEmail, String firstName,
                                                  String contentType, String contentPreview,
                                                  String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("contentType", contentType);
        context.setVariable("contentPreview", contentPreview);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(userId, toEmail, "Your content has been moderated - FosterLink", CONTENT_MODERATED_TEMPLATE, context);
    }
}
