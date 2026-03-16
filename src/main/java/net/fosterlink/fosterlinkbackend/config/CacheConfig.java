package net.fosterlink.fosterlinkbackend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                // Ban status per user ID — evicted explicitly on ban/unban, 5-min TTL safety net
                build("bannedUsers",   10_000, 5),
                // Full UserDetails per email — evicted on any user mutation, 2-min TTL safety net
                build("userDetails",   10_000, 2),
                // Approved FAQ preview pages — evicted on any FAQ state change
                build("faqApprovedPreviews", 100, 10),
                // Approved agency row pages — evicted on any agency state change
                build("agencyApprovedRows",  100, 10),
                // Public profile metadata per userId — evicted on profile updates
                build("profileMetadata",     10_000, 5)
        ));
        return manager;
    }

    private CaffeineCache build(String name, int maxSize, int ttlMinutes) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .build());
    }
}
