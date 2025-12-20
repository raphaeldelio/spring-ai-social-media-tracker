package com.redis.socialmediatracker.slack;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import org.springframework.data.annotation.Id;

/**
 * Redis document representing a processed Slack event.
 * Used for event deduplication to prevent processing the same event multiple times.
 * <p>
 * TTL is set to 1 hour (3600 seconds) - Slack typically retries within minutes,
 * so 1 hour is a safe retention period.
 */
@Document(value = "processed-event", indexName = "processedEventIdx", timeToLive = 3600)
public class ProcessedEvent {
    
    /**
     * The unique event ID from Slack (e.g., "Ev1234567890")
     */
    @Id
    private String eventId;
    
    /**
     * Timestamp when the event was first processed (epoch milliseconds)
     */
    @Indexed
    private Long firstSeenTimestamp;
    
    /**
     * Event type (e.g., "app_mention", "message")
     */
    @Indexed
    private String eventType;
    
    /**
     * Team ID where the event originated
     */
    @Indexed
    private String teamId;

    // Constructors
    
    public ProcessedEvent() {
    }
    
    public ProcessedEvent(String eventId, Long firstSeenTimestamp, String eventType, String teamId) {
        this.eventId = eventId;
        this.firstSeenTimestamp = firstSeenTimestamp;
        this.eventType = eventType;
        this.teamId = teamId;
    }

    // Getters and Setters
    
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getFirstSeenTimestamp() {
        return firstSeenTimestamp;
    }

    public void setFirstSeenTimestamp(Long firstSeenTimestamp) {
        this.firstSeenTimestamp = firstSeenTimestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    @Override
    public String toString() {
        return "ProcessedEvent{" +
                "eventId='" + eventId + '\'' +
                ", firstSeenTimestamp=" + firstSeenTimestamp +
                ", eventType='" + eventType + '\'' +
                ", teamId='" + teamId + '\'' +
                '}';
    }
}

