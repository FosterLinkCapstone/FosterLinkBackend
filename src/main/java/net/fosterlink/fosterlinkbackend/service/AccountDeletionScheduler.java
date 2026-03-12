package net.fosterlink.fosterlinkbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for account deletion lifecycle management.
 * Auto-approvals run every hour; 7-day warnings run once per day at midnight.
 */
@Component
public class AccountDeletionScheduler {

    @Autowired
    private AccountDeletionService accountDeletionService;

    @Scheduled(cron = "0 0 * * * *")
    public void runAutoApprovals() {
        accountDeletionService.processAutoApprovals();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void runAutoApprovalWarnings() {
        accountDeletionService.processAutoApprovalWarnings();
    }
}
