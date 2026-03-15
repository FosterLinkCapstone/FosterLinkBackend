package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Service for user moderation operations such as lifting expired temporary restrictions.
 */
@Service
public class UserModerationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BanStatusService banStatusService;

    /**
     * Finds all users whose temporary restriction has expired (restrictedUntil is in the past)
     * and clears their restriction. Users with a null restrictedUntil are permanently restricted
     * and are not affected.
     *
     * M-2: replaces N individual save() calls with a single bulk UPDATE, then iterates only
     * for per-user cache evictions.
     */
    @Transactional
    public void processExpiredRestrictions() {
        Date now = new Date();
        List<UserEntity> expired = userRepository.findExpiredRestrictions(now);
        if (expired.isEmpty()) return;
        userRepository.clearExpiredRestrictions(now);
        for (UserEntity user : expired) {
            banStatusService.evict(user.getEmail());
            banStatusService.evictProfileMetadata(user.getId());
        }
    }
}
