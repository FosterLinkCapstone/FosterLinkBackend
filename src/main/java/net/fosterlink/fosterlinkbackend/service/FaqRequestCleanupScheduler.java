package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.repositories.FAQRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purges faq_request rows older than 90 days. Runs daily at 04:30.
 */
@Component
public class FaqRequestCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(FaqRequestCleanupScheduler.class);

    @Autowired
    private FAQRequestRepository faqRequestRepository;

    @Scheduled(cron = "0 30 4 * * *")
    public void deleteExpiredFaqRequests() {
        int deleted = faqRequestRepository.deleteOlderThan90Days();
        log.info("FaqRequestCleanupScheduler: deleted {} FAQ requests older than 90 days", deleted);
    }
}
