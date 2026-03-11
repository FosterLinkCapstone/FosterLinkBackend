package net.fosterlink.fosterlinkbackend.mail.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

/**
 * Mail service for agency-related operations.
 * Add email-sending methods here when needed (e.g. agency approval/denial, deletion requests).
 */
@Service
public class AgencyMailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public AgencyMailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
}
