package com.redis.socialmediatracker.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Thread-safe service for deduplicating Slack events using Redis.
 * <p>
 * Slack may send duplicate events in several scenarios:
 * 1. Network retries if we don't respond quickly enough
 * 2. Multiple event types for the same user action (app_mention + message)
 * 3. Slack's internal retry logic
 * <p>
 * This service uses Redis OM Spring to store processed event IDs with automatic TTL-based cleanup.
 * Events are stored in Redis with a 1-hour TTL and automatically expire.
 */
@Service
public class EventDeduplicationService {

    private static final Logger logger = LoggerFactory.getLogger(EventDeduplicationService.class);

    private final ProcessedEventRepository processedEventRepository;

    public EventDeduplicationService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Check if an event has already been processed, and mark it as processed if not.
     * This operation uses Redis to ensure thread-safety across multiple instances.
     *
     * @param eventId The unique event ID from Slack
     * @param eventType The event type (e.g., "app_mention", "message")
     * @param teamId The team ID where the event originated
     * @return true if this is a NEW event (should be processed), false if it's a duplicate
     */
    public boolean isNewEvent(String eventId, String eventType, String teamId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            logger.warn("⚠️ Received null or empty event ID");
            return true; // Process it anyway to avoid losing events
        }

        try {
            // Check if event already exists in Redis
            ProcessedEvent existingEvent = processedEventRepository.findByEventId(eventId);

            if (existingEvent == null) {
                // This is a new event - store it in Redis
                long currentTime = Instant.now().toEpochMilli();
                ProcessedEvent newEvent = new ProcessedEvent(eventId, currentTime, eventType, teamId);
                processedEventRepository.save(newEvent);

                logger.debug("✅ New event: {}", eventId);
                return true;
            } else {
                long currentTime = Instant.now().toEpochMilli();
                long timeSinceFirstSeen = currentTime - existingEvent.getFirstSeenTimestamp();
                logger.info("🔄 Duplicate event detected: {} (first seen {} ms ago)",
                    eventId, timeSinceFirstSeen);
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Error checking event deduplication for {}: {}", eventId, e.getMessage());
            return true;
        }
    }
}

