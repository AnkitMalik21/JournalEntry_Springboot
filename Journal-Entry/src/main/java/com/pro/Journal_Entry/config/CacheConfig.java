package com.pro.Journal_Entry.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Cache Configuration
 *
 * CACHE NAMES USED:
 * - "journals" - Individual journal entries
 * - "calendar" - Calendar month view
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {
    /**
     * Clear all caches at midnight
     * Prevents stale data accumulation
     * Cron: "0 0 0 * * ?" = Every day at 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @CacheEvict(value = {"journals","calendar"},allEntries = true)
    public void clearCacheMidnight(){
        //Caches are automatically cleared bt @CacheEvict
    }
}
