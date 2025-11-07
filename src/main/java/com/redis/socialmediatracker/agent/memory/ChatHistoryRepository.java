package com.redis.socialmediatracker.agent.memory;

import com.redis.om.spring.repository.RedisDocumentRepository;

public interface ChatHistoryRepository extends RedisDocumentRepository<ChatHistory, String> {
}