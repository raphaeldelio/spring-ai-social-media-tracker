package com.redis.socialmediatracker.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe service for deduplicating Slack events.
 * 
 * Slack may send duplicate events in several scenarios:
 * 1. Network retries if we don't respond quickly enough
 * 2. Multiple event types for the same user action (app_mention + message)
 * 3. Slack's internal retry logic
 * 
 * This service maintains an in-memory cache of processed event IDs with automatic cleanup.
 */
@Service
public class EventDeduplicationService {

    private static final Logger logger = LoggerFactory.getLogger(EventDeduplicationService.class);
    
    /**
     * Thread-safe map to store processed event IDs with their timestamps.
     * ConcurrentHashMap provides thread-safe operations without external synchronization.
     */
    private final ConcurrentHashMap<String, Long> processedEvents = new ConcurrentHashMap<>();
    
    /**
     * How long to keep event IDs in memory (in milliseconds).
     * Default: 1 hour - Slack typically retries within minutes, so 1 hour is safe.
     */
    private static final long RETENTION_PERIOD_MS = TimeUnit.HOURS.toMillis(1);
    
    /**
     * How often to run cleanup of old event IDs (in minutes).
     */
    private static final long CLEANUP_INTERVAL_MINUTES = 10;
    
    private final ScheduledExecutorService cleanupScheduler;

    public EventDeduplicationService() {
        // Initialize cleanup scheduler
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "event-dedup-cleanup");
            thread.setDaemon(true); // Don't prevent JVM shutdown
            return thread;
        });
        
        // Schedule periodic cleanup
        cleanupScheduler.scheduleAtFixedRate(
            this::cleanupOldEvents,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        logger.info("✅ Event deduplication service initialized");
        logger.info("   Retention period: {} minutes", TimeUnit.MILLISECONDS.toMinutes(RETENTION_PERIOD_MS));
        logger.info("   Cleanup interval: {} minutes", CLEANUP_INTERVAL_MINUTES);
    }

    /**
     * Check if an event has already been processed, and mark it as processed if not.
     * This operation is atomic and thread-safe.
     * 
     * @param eventId The unique event ID from Slack
     * @return true if this is a NEW event (should be processed), false if it's a duplicate
     */
    public boolean isNewEvent(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            logger.warn("⚠️ Received null or empty event ID");
            return true; // Process it anyway to avoid losing events
        }
        
        long currentTime = Instant.now().toEpochMilli();
        
        // putIfAbsent is atomic - returns null if the key was not present
        // This ensures only ONE thread can mark an event as "new"
        Long previousValue = processedEvents.putIfAbsent(eventId, currentTime);
        
        if (previousValue == null) {
            // This is a new event - we successfully added it
            logger.debug("✅ New event: {}", eventId);
            return true;
        } else {
            // This event was already processed
            long timeSinceFirstSeen = currentTime - previousValue;
            logger.info("🔄 Duplicate event detected: {} (first seen {} ms ago)", 
                eventId, timeSinceFirstSeen);
            return false;
        }
    }

    /**
     * Remove old event IDs from memory to prevent unbounded growth.
     * This runs periodically in the background.
     */
    private void cleanupOldEvents() {
        try {
            long currentTime = Instant.now().toEpochMilli();
            long cutoffTime = currentTime - RETENTION_PERIOD_MS;
            int initialSize = processedEvents.size();
            
            // Remove entries older than retention period
            processedEvents.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
            
            int removedCount = initialSize - processedEvents.size();
            if (removedCount > 0) {
                logger.info("🧹 Cleaned up {} old event IDs. Current cache size: {}", 
                    removedCount, processedEvents.size());
            }
        } catch (Exception e) {
            logger.error("❌ Error during event cleanup", e);
        }
    }

    /**
     * Get the current number of tracked events (for monitoring/debugging).
     */
    public int getCacheSize() {
        return processedEvents.size();
    }

    /**
     * Clear all tracked events (useful for testing).
     */
    public void clear() {
        processedEvents.clear();
        logger.info("🗑️ Event cache cleared");
    }

    /**
     * Shutdown the cleanup scheduler gracefully.
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

