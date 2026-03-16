package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

/**
 * Centralized cache management for user-keyed caches.
 *
 * Ban status lookups are served from an in-memory Caffeine cache keyed by user ID.
 * Entries expire after 5 minutes as a safety net and are explicitly evicted when
 * a user is banned, unbanned, restricted, or unrestricted so changes take effect immediately.
 *
 * UserDetails cache mirrors email-keyed entries; both are evicted together on ban/unban so
 * the filter's loadUserByUsername call is also covered by the same eviction paths.
 */
@Service
public class BanStatusService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Returns whether the user with the given database ID is currently banned.
     * The result is cached in the "bannedUsers" Caffeine cache, keyed by user ID.
     * A DB query is only issued on a cache miss.
     */
    @Cacheable(value = "bannedUsers", key = "#userId")
    public boolean isBanned(long userId) {
        Boolean banned = userRepository.isBannedById(userId);
        return Boolean.TRUE.equals(banned);
    }

    /**
     * Evicts both the ban status (keyed by user ID) and the cached UserDetails (keyed by
     * email) for the given user. Must be called after any ban, unban, restrict, or unrestrict
     * operation.
     */
    @Caching(evict = {
            @CacheEvict(value = "bannedUsers",  key = "#userId"),
            @CacheEvict(value = "userDetails",  key = "#email")
    })
    public void evict(long userId, String email) {
    }

    /**
     * Evicts only the cached UserDetails for the given email.
     * Use after profile mutations (password change, email update, account deletion)
     * that do not change ban status.
     */
    @CacheEvict(value = "userDetails", key = "#email")
    public void evictUserDetails(String email) {
    }

    /**
     * Evicts the cached profile metadata for the given userId.
     * Use after any mutation that changes publicly visible profile data.
     */
    @CacheEvict(value = "profileMetadata", key = "#userId")
    public void evictProfileMetadata(int userId) {
    }
}
