package net.fosterlink.fosterlinkbackend.mail.service;

import net.fosterlink.fosterlinkbackend.mail.CheckEmailPreference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Mail service for thread-related operations.
 * Uses external HTML templates under templates/mail/.
 */
@Service
public class ThreadMailService {

    private static final String THREAD_REPLY_TEMPLATE = "mail/thread-reply";

    private final MailSendHelper mailSendHelper;

    public ThreadMailService(MailSendHelper mailSendHelper) {
        this.mailSendHelper = mailSendHelper;
    }

    /**
     * Sent to the thread author when someone else replies to their thread.
     * Not sent when the author replies to their own thread.
     */
    @CheckEmailPreference(value = "THREAD_REPLY", uiName = "New reply to your thread")
    @Async
    public void sendThreadReplyNotification(int userId, String toEmail, String firstName,
                                             String threadTitle, String replyPreview,
                                             String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("threadTitle", threadTitle);
        context.setVariable("replyPreview", replyPreview);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "Someone replied to your thread - FosterLink", THREAD_REPLY_TEMPLATE, context);
    }
}
