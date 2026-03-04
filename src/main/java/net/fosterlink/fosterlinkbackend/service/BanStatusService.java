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
 * Ban status lookups are served from an in-memory Caffeine cache keyed by email.
 * Entries expire after 5 minutes as a safety net and are explicitly evicted when
 * a user is banned or unbanned so changes take effect immediately.
 *
 * UserDetails cache mirrors the same keys so the filter's loadUserByUsername call
 * is also covered by the same eviction paths.
 */
@Service
public class BanStatusService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Returns whether the user with the given email is currently banned.
     * The result is cached in the "bannedUsers" Caffeine cache, keyed by email.
     * A DB query is only issued on a cache miss.
     */
    @Cacheable(value = "bannedUsers", key = "#email")
    public boolean isBanned(String email) {
        Boolean banned = userRepository.isBannedByEmail(email);
        return Boolean.TRUE.equals(banned);
    }

    /**
     * Evicts both the ban status and the cached UserDetails for the given email.
     * Must be called after any ban or unban operation.
     */
    @Caching(evict = {
            @CacheEvict(value = "bannedUsers",  key = "#email"),
            @CacheEvict(value = "userDetails",  key = "#email")
    })
    public void evict(String email) {
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
