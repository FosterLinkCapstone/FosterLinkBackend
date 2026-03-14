package net.fosterlink.fosterlinkbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that hard-deletes forum posts user-soft-deleted 90+ days ago.
 * Runs daily at 02:00 to avoid overlap with the audit-log cleanup at 01:30.
 */
@Component
public class PostCleanupScheduler {

    @Autowired
    private PostCleanupService postCleanupService;

    @Scheduled(cron = "0 0 2 * * *")
    public void runPostCleanup() {
        postCleanupService.purgeExpiredSoftDeletedPosts();
    }
}
