package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.repositories.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes audit log rows that have passed their 730-day (2-year) retention period.
 * Runs daily at 01:30 to avoid peak traffic hours.
 */
@Component
public class AuditLogCleanupScheduler {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Scheduled(cron = "0 30 1 * * *")
    public void purgeExpiredAuditLogs() {
        auditLogRepository.deleteExpiredAuditLogs();
    }
}
