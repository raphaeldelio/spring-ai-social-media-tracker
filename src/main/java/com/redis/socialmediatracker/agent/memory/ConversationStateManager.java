package com.redis.socialmediatracker.agent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages conversation state for multi-turn interactions with agents.
 * Stores state in-memory (could be moved to Redis for persistence).
 */
@Service
public class ConversationStateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationStateManager.class);
    private static final long CONVERSATION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes
    
    // Map of (teamId + channel + threadTs) -> ConversationState
    private final Map<String, ConversationState> activeConversations = new ConcurrentHashMap<>();
    
    /**
     * Create a unique key for a conversation based on team, channel, and thread.
     */
    private String createKey(String teamId, String channel, String threadTs) {
        return teamId + ":" + channel + ":" + (threadTs != null ? threadTs : "");
    }
    
    /**
     * Start a new conversation or get existing one.
     */
    public ConversationState getOrCreateConversation(String teamId, String channel, String threadTs) {
        String key = createKey(teamId, channel, threadTs);
        
        ConversationState state = activeConversations.get(key);
        
        if (state != null) {
            // Check if conversation has timed out
            long age = System.currentTimeMillis() - state.getLastActivityTimestamp();
            if (age > CONVERSATION_TIMEOUT_MS) {
                logger.info("Conversation {} timed out, creating new one", state.getConversationId());
                activeConversations.remove(key);
            } else {
                state.updateActivity();
                logger.info("Continuing existing conversation: {}", state.getConversationId());
                return state;
            }
        }
        
        // Create new conversation
        String conversationId = java.util.UUID.randomUUID().toString();
        state = new ConversationState(conversationId, teamId, channel, threadTs);
        activeConversations.put(key, state);
        logger.info("Created new conversation: {}", conversationId);
        
        return state;
    }
    
    /**
     * Update an existing conversation state.
     */
    public void updateConversation(ConversationState state) {
        String key = createKey(state.getTeamId(), state.getChannel(), state.getThreadTs());
        state.updateActivity();
        activeConversations.put(key, state);
    }
    
    /**
     * Remove a conversation (when completed or cancelled).
     */
    public void removeConversation(String teamId, String channel, String threadTs) {
        String key = createKey(teamId, channel, threadTs);
        ConversationState removed = activeConversations.remove(key);
        if (removed != null) {
            logger.info("Removed conversation: {}", removed.getConversationId());
        }
    }
    
    /**
     * Clean up old conversations (should be called periodically).
     */
    public void cleanupOldConversations() {
        long now = System.currentTimeMillis();
        activeConversations.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().getLastActivityTimestamp();
            if (age > CONVERSATION_TIMEOUT_MS) {
                logger.info("Cleaning up old conversation: {}", entry.getValue().getConversationId());
                return true;
            }
            return false;
        });
    }
}

