package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.repositories.AccountDeletionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for account deletion lifecycle management.
 * Auto-approvals run every hour; 7-day warnings run once per day at midnight.
 * Retention cleanup runs daily at 03:00.
 */
@Component
public class AccountDeletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionScheduler.class);

    @Autowired
    private AccountDeletionService accountDeletionService;

    @Autowired
    private AccountDeletionRequestRepository accountDeletionRequestRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void runAutoApprovals() {
        accountDeletionService.processAutoApprovals();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void runAutoApprovalWarnings() {
        accountDeletionService.processAutoApprovalWarnings();
    }

    /**
     * Retention cleanup — runs daily at 03:00.
     * 1. Nullifies requested_by_email_hash on rows where deletion was executed more than 30 days ago.
     * 2. Hard-deletes rows where deletion was executed more than 7 years ago.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runRetentionCleanup() {
        int nullified = accountDeletionRequestRepository.nullifyExpiredEmailHashes();
        log.info("AccountDeletionScheduler: nullified {} expired email hashes", nullified);
        int deleted = accountDeletionRequestRepository.deleteExpiredDeletionRecords();
        log.info("AccountDeletionScheduler: deleted {} expired deletion records (>7 years)", deleted);
    }
}
