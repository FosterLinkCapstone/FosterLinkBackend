package net.fosterlink.fosterlinkbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that runs every hour and automatically approves any account deletion requests
 * that have passed their auto-approval deadline.
 */
@Component
public class AccountDeletionScheduler {

    @Autowired
    private AccountDeletionService accountDeletionService;

    @Scheduled(cron = "0 0 * * * *")
    public void runAutoApprovals() {
        accountDeletionService.processAutoApprovals();
    }
}
