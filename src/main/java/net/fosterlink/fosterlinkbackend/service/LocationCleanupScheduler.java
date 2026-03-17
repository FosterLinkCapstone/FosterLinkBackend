package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.repositories.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cleans up orphaned location rows — those no longer referenced by any agency.
 * Runs daily at 04:00.
 */
@Component
public class LocationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LocationCleanupScheduler.class);

    @Autowired
    private LocationRepository locationRepository;

    @Scheduled(cron = "0 0 4 * * *")
    public void deleteOrphanedLocations() {
        int deleted = locationRepository.deleteOrphanedLocations();
        log.info("LocationCleanupScheduler: deleted {} orphaned location rows", deleted);
    }
}
