package com.redis.socialmediatracker.slack;

import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing Slack OAuth tokens in Redis.
 */
@Repository
public interface SlackTokenRepository extends RedisDocumentRepository<SlackToken, String> {
    
    /**
     * Find a token by Slack team ID.
     * @param teamId The Slack team/workspace ID
     * @return Optional containing the token if found
     */
    Optional<SlackToken> findByTeamId(String teamId);
}

