package net.fosterlink.fosterlinkbackend.mail.service;

import net.fosterlink.fosterlinkbackend.mail.CheckEmailPreference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Mail service for FAQ-related operations (approval, denial, suggestion feedback).
 * Uses external HTML templates under templates/mail/.
 */
@Service
public class FaqMailService {

    private static final String FAQ_APPROVED_TEMPLATE = "mail/faq-approved";
    private static final String FAQ_DENIED_TEMPLATE = "mail/faq-denied";
    private static final String FAQ_SUGGESTION_RECEIVED_TEMPLATE = "mail/faq-suggestion-received";
    private static final String FAQ_SUGGESTION_ANSWERED_TEMPLATE = "mail/faq-suggestion-answered";

    private final MailSendHelper mailSendHelper;

    public FaqMailService(MailSendHelper mailSendHelper) {
        this.mailSendHelper = mailSendHelper;
    }

    /** Sent to the FAQ author after an administrator approves their FAQ. */
    @CheckEmailPreference(value = "FAQ_APPROVED", uiName = "FAQ approved or denied")
    @Async
    public void sendFaqApproved(int userId, String toEmail, String firstName, String faqTitle, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("faqTitle", faqTitle);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your FAQ has been approved - FosterLink", FAQ_APPROVED_TEMPLATE, context);
    }

    /** Sent to the FAQ author after an administrator denies their FAQ. */
    @CheckEmailPreference(value = "FAQ_APPROVED", uiName = "FAQ approved or denied", userIdParamIndex = 0)
    @Async
    public void sendFaqDenied(int userId, String toEmail, String firstName, String faqTitle, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("faqTitle", faqTitle);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your FAQ was not approved - FosterLink", FAQ_DENIED_TEMPLATE, context);
    }

    /**
     * Sent to a user who submitted an FAQ suggestion, confirming the suggestion was received.
     * The requester's user ID is the first param so the preference check applies to them.
     */
    @CheckEmailPreference(value = "FAQ_SUGGESTION_ANSWERED", uiName = "Your FAQ suggestion was answered")
    @Async
    public void sendFaqSuggestionReceived(int userId, String toEmail, String firstName, String suggestedTopic, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("suggestedTopic", suggestedTopic);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "We received your FAQ suggestion - FosterLink", FAQ_SUGGESTION_RECEIVED_TEMPLATE, context);
    }

    /**
     * Sent to FAQ authors/admins when a new FAQ suggestion is submitted, so they know to review it.
     * The recipient's user ID is the first param.
     */
    @CheckEmailPreference(value = "FAQ_SUGGESTION_RECEIVED", uiName = "New FAQ suggestion received")
    @Async
    public void sendFaqSuggestionReceivedNotice(int recipientUserId, String toEmail, String recipientFirstName,
                                                 String suggestedTopic, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(recipientFirstName));
        context.setVariable("suggestedTopic", suggestedTopic);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(recipientUserId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "New FAQ suggestion submitted - FosterLink", FAQ_SUGGESTION_RECEIVED_TEMPLATE, context);
    }
}
