package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.repositories.AgencyDeletionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for agency deletion lifecycle management.
 * Auto-approvals run every hour; 7-day warnings run once per day at midnight.
 * Retention cleanup runs daily at 03:30.
 */
@Component
public class AgencyDeletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgencyDeletionScheduler.class);

    @Autowired
    private AgencyDeletionService agencyDeletionService;

    @Autowired
    private AgencyDeletionRequestRepository agencyDeletionRequestRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void runAutoApprovals() {
        agencyDeletionService.processAutoApprovals();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void runAutoApprovalWarnings() {
        agencyDeletionService.processAutoApprovalWarnings();
    }

    /**
     * Retention cleanup — runs daily at 03:30.
     * Deletes approved agency_deletion_request rows that are more than 1 year old.
     */
    @Scheduled(cron = "0 30 3 * * *")
    public void runRetentionCleanup() {
        int deleted = agencyDeletionRequestRepository.deleteExpiredApprovedRequests();
        log.info("AgencyDeletionScheduler: deleted {} expired approved agency deletion records (>1 year)", deleted);
    }
}
