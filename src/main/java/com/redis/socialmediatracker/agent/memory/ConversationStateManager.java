package com.redis.socialmediatracker.agent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Manages conversation state for multi-turn interactions with agents.
 * Stores state in Redis using Redis OM Spring for persistence across container restarts.
 * <p>
 * Benefits of Redis storage:
 * - Survives container restarts (important for Cloud Run)
 * - Automatic TTL (30 minutes) via @Document annotation
 * - Queryable by team, channel, thread
 * - Scales horizontally if multiple instances are deployed
 */
@Service
public class ConversationStateManager {

    private static final Logger logger = LoggerFactory.getLogger(ConversationStateManager.class);

    private final ConversationStateRepository conversationStateRepository;

    public ConversationStateManager(ConversationStateRepository conversationStateRepository) {
        this.conversationStateRepository = conversationStateRepository;
    }

    /**
     * Create a unique key for a conversation based on team, channel, and thread.
     */
    private String createKey(String teamId, String channel, String threadTs) {
        return teamId + ":" + channel + ":" + (threadTs != null ? threadTs : "");
    }
    
    /**
     * Start a new conversation or get existing one.
     * Looks up in Redis first, creates new if not found or expired.
     */
    public ConversationState getOrCreateConversation(String teamId, String channel, String threadTs) {
        String key = createKey(teamId, channel, threadTs);

        // Try to find existing conversation in Redis
        Optional<ConversationState> existingState = conversationStateRepository.findById(key);

        if (existingState.isPresent()) {
            ConversationState state = existingState.get();
            state.updateActivity();
            conversationStateRepository.save(state);
            logger.info("Continuing existing conversation: {} (from Redis)", state.getConversationId());
            return state;
        }

        // Create new conversation
        String conversationId = UUID.randomUUID().toString();
        ConversationState state = new ConversationState(conversationId, teamId, channel, threadTs);
        conversationStateRepository.save(state);
        logger.info("Created new conversation: {} (saved to Redis)", conversationId);

        return state;
    }
    
    /**
     * Update an existing conversation state in Redis.
     */
    public void updateConversation(ConversationState state) {
        state.updateActivity();
        conversationStateRepository.save(state);
        logger.debug("Updated conversation {} in Redis", state.getConversationId());
    }
}

