package com.redis.socialmediatracker.agent.memory;

import com.redis.om.spring.annotations.Document;
import org.springframework.data.annotation.Id;

import java.util.List;

@Document(value = "chat-history", indexName = "chatHistoryIdx")
public class ChatHistory {

    @Id
    private String id;
    private List<StoredMessage> messages;

    public ChatHistory() {
    }

    public ChatHistory(String id, List<StoredMessage> messages) {
        this.id = id;
        this.messages = messages;
    }

    public String getId() {
        return id;
    }

    public List<StoredMessage> getMessages() {
        return messages;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMessages(List<StoredMessage> messages) {
        this.messages = messages;
    }
}