package com.redis.socialmediatracker.slack;

import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends RedisDocumentRepository<ProcessedEvent, String> {
    ProcessedEvent findByEventId(String eventId);
}

