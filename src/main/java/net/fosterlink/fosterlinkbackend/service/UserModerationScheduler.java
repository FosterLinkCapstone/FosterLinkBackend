package net.fosterlink.fosterlinkbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that runs every hour and automatically lifts temporary user restrictions
 * whose restrictedUntil date has passed.
 */
@Component
public class UserModerationScheduler {

    @Autowired
    private UserModerationService userModerationService;

    @Scheduled(cron = "0 0 * * * *")
    public void runExpiredRestrictionLifts() {
        userModerationService.processExpiredRestrictions();
    }
}
