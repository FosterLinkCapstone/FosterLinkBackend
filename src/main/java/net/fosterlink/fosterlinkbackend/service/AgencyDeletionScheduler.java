package net.fosterlink.fosterlinkbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for agency deletion lifecycle management.
 * Auto-approvals run every hour; 7-day warnings run once per day at midnight.
 */
@Component
public class AgencyDeletionScheduler {

    @Autowired
    private AgencyDeletionService agencyDeletionService;

    @Scheduled(cron = "0 0 * * * *")
    public void runAutoApprovals() {
        agencyDeletionService.processAutoApprovals();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void runAutoApprovalWarnings() {
        agencyDeletionService.processAutoApprovalWarnings();
    }
}
