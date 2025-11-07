package com.redis.socialmediatracker.agent.memory;

import com.redis.om.spring.search.stream.EntityStream;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private final ChatHistoryRepository chatHistoryRepository;
    private final EntityStream entityStream;

    public RedisChatMemoryRepository(ChatHistoryRepository chatHistoryRepository, EntityStream entityStream) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.entityStream = entityStream;
    }

    @Override
    public List<String> findConversationIds() {
        return entityStream.of(ChatHistory.class)
            .map(ChatHistory::getId)
            .collect(Collectors.toList())
            .stream()
            .filter(id -> id != null)
        .collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Optional<ChatHistory> optHist = chatHistoryRepository.findById(conversationId);
        return optHist.map(chatHistory -> chatHistory.getMessages()
                .stream()
                .map(StoredMessage::toAi)
                .collect(Collectors.toList())).orElse(Collections.emptyList());
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<StoredMessage> storedMessages = messages.stream()
            .map(msg -> {
        if (msg instanceof AssistantMessage) {
            AssistantMessage am = (AssistantMessage) msg;
            return new StoredMessage(
                    am.getText() != null ? am.getText() : "",
            am.getMetadata(),
            am.getToolCalls(),
            null,
            am.getMedia(),
            MessageType.ASSISTANT
            );
        } else if (msg instanceof UserMessage) {
            UserMessage um = (UserMessage) msg;
            return new StoredMessage(
                    um.getText(),
            um.getMetadata(),
            null,
            null,
            um.getMedia(),
            MessageType.USER
            );
        } else if (msg instanceof SystemMessage) {
            SystemMessage sm = (SystemMessage) msg;
            return new StoredMessage(
                    sm.getText(),
            sm.getMetadata(),
            null,
            null,
            null,
            MessageType.SYSTEM
            );
        } else if (msg instanceof ToolResponseMessage) {
            ToolResponseMessage trm = (ToolResponseMessage) msg;
            return new StoredMessage(
                    "",
            trm.getMetadata(),
            null,
            trm.getResponses(),
            null,
            MessageType.TOOL
            );
        } else {
            throw new IllegalArgumentException("Unknown message type: " + msg.getClass().getCanonicalName());
        }
    })
        .collect(Collectors.toList());

        Optional<ChatHistory> optHist = chatHistoryRepository.findById(conversationId);
        if (optHist.isPresent()) {
            ChatHistory existing = optHist.get();
            ChatHistory updated = new ChatHistory(existing.getId(), storedMessages);
            chatHistoryRepository.save(updated);
        } else {
            ChatHistory newHist = new ChatHistory(conversationId, storedMessages);
            chatHistoryRepository.save(newHist);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        chatHistoryRepository.deleteById(conversationId);
    }
}