package com.redis.socialmediatracker.agent.memory;

import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing conversation state in Redis.
 * Uses Redis OM Spring for automatic persistence and querying.
 */
@Repository
public interface ConversationStateRepository extends RedisDocumentRepository<ConversationState, String> {
    
    /**
     * Find a conversation state by team ID, channel, and thread timestamp.
     * This is the primary lookup method for active conversations.
     * 
     * @param teamId The Slack team ID
     * @param channel The Slack channel ID
     * @param threadTs The Slack thread timestamp (can be null for non-threaded messages)
     * @return Optional containing the conversation state if found
     */
    Optional<ConversationState> findByTeamIdAndChannelAndThreadTs(String teamId, String channel, String threadTs);
    
    /**
     * Find all conversations for a specific team.
     * Useful for monitoring and debugging.
     * 
     * @param teamId The Slack team ID
     * @return List of conversation states for the team
     */
    List<ConversationState> findByTeamId(String teamId);
    
    /**
     * Find conversations that are older than a specific timestamp.
     * Used for cleanup of stale conversations.
     * 
     * @param timestamp The cutoff timestamp
     * @return List of old conversation states
     */
    List<ConversationState> findByLastActivityTimestampLessThan(long timestamp);
}

