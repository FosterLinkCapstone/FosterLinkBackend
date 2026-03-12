package net.fosterlink.fosterlinkbackend.mail.service;

import net.fosterlink.fosterlinkbackend.mail.CheckEmailPreference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Mail service for agency-related operations (approval, denial, deletion requests).
 * Uses external HTML templates under templates/mail/.
 */
@Service
public class AgencyMailService {

    private static final String AGENCY_SUBMITTED_TEMPLATE = "mail/agency-submitted";
    private static final String AGENCY_APPROVED_TEMPLATE = "mail/agency-approved";
    private static final String AGENCY_DENIED_TEMPLATE = "mail/agency-denied";
    private static final String AGENCY_DELETION_APPROVED_TEMPLATE = "mail/agency-deletion-approved";
    private static final String AGENCY_DELETION_DENIED_TEMPLATE = "mail/agency-deletion-denied";

    private final MailSendHelper mailSendHelper;

    public AgencyMailService(MailSendHelper mailSendHelper) {
        this.mailSendHelper = mailSendHelper;
    }

    /** Sent to the agency representative after they submit a new agency for review. */
    @CheckEmailPreference(value = "AGENCY_SUBMITTED", uiName = "Agency submitted for review")
    @Async
    public void sendAgencySubmittedConfirmation(int userId, String toEmail, String firstName, String agencyName, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("agencyName", agencyName);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your agency has been submitted for review - FosterLink", AGENCY_SUBMITTED_TEMPLATE, context);
    }

    /** Sent to the agency representative after an administrator approves their agency. */
    @CheckEmailPreference(value = "AGENCY_APPROVED", uiName = "Agency approved or denied")
    @Async
    public void sendAgencyApproved(int userId, String toEmail, String firstName, String agencyName, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("agencyName", agencyName);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your agency has been approved - FosterLink", AGENCY_APPROVED_TEMPLATE, context);
    }

    /** Sent to the agency representative after an administrator denies their agency. */
    @CheckEmailPreference(value = "AGENCY_APPROVED", uiName = "Agency approved or denied", userIdParamIndex = 0)
    @Async
    public void sendAgencyDenied(int userId, String toEmail, String firstName, String agencyName, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("agencyName", agencyName);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your agency application was not approved - FosterLink", AGENCY_DENIED_TEMPLATE, context);
    }

    /** Sent to the agency representative after an administrator approves their deletion request. */
    @CheckEmailPreference(value = "AGENCY_DELETION_RESOLVED", uiName = "Agency deletion request resolved")
    @Async
    public void sendAgencyDeletionApproved(int userId, String toEmail, String firstName, String agencyName, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("agencyName", agencyName);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your agency deletion request has been approved - FosterLink", AGENCY_DELETION_APPROVED_TEMPLATE, context);
    }

    /** Sent to the agency representative after an administrator denies their deletion request. */
    @CheckEmailPreference(value = "AGENCY_DELETION_RESOLVED", uiName = "Agency deletion request resolved", userIdParamIndex = 0)
    @Async
    public void sendAgencyDeletionDenied(int userId, String toEmail, String firstName, String agencyName, String unsubscribeToken) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("greetingName", mailSendHelper.greetingName(firstName));
        context.setVariable("agencyName", agencyName);
        context.setVariable("unsubscribeUrl", mailSendHelper.buildUnsubscribeUrl(userId, unsubscribeToken));
        mailSendHelper.sendTemplatedEmail(toEmail, "Your agency deletion request has been denied - FosterLink", AGENCY_DELETION_DENIED_TEMPLATE, context);
    }
}
