package com.redis.socialmediatracker.agent.memory;

import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing conversation state in Redis.
 * Uses Redis OM Spring for automatic persistence and querying.
 */
@Repository
public interface ConversationStateRepository extends RedisDocumentRepository<ConversationState, String> {

    /**
     * Find all conversations that are currently running.
     * Used for startup recovery to resume interrupted workflows.
     *
     * @param isRunning Whether the conversation is running
     * @return List of running conversation states
     */
    List<ConversationState> findByIsRunning(boolean isRunning);
}

