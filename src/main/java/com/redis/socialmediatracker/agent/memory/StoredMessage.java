package com.redis.socialmediatracker.agent.memory;

import org.springframework.ai.chat.messages.*;
import org.springframework.ai.content.Media;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class StoredMessage {

    private String text = "";
    private Map<String, Object> metadata = Collections.emptyMap();
    private List<AssistantMessage.ToolCall> toolCalls;
    private List<ToolResponseMessage.ToolResponse> toolResponses;
    private List<Media> media;
    private MessageType messageType;

    public StoredMessage() {
    }

    public StoredMessage(String text,
    Map<String, Object> metadata,
    List<AssistantMessage.ToolCall> toolCalls,
    List<ToolResponseMessage.ToolResponse> toolResponses,
    List<Media> media,
    MessageType messageType) {
        this.text = text;
        this.metadata = metadata != null ? metadata : Collections.emptyMap();
        this.toolCalls = toolCalls;
        this.toolResponses = toolResponses;
        this.media = media;
        this.messageType = messageType;
    }

    public String getText() {
        return text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<AssistantMessage.ToolCall> getToolCalls() {
        return toolCalls;
    }

    public List<ToolResponseMessage.ToolResponse> getToolResponses() {
        return toolResponses;
    }

    public List<Media> getMedia() {
        return media;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public void setToolResponses(List<ToolResponseMessage.ToolResponse> toolResponses) {
        this.toolResponses = toolResponses;
    }

    public void setMedia(List<Media> media) {
        this.media = media;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Message toAi() {
        if (messageType == null) {
            throw new IllegalStateException("MessageType must not be null");
        }

        switch (messageType) {
            case USER:
            return UserMessage.builder()
                .text(text)
                .metadata(metadata)
                .media(media != null ? media : Collections.emptyList())
            .build();

            case ASSISTANT:
            return new AssistantMessage(
                    text,
            metadata,
            toolCalls != null ? toolCalls : Collections.emptyList(),
            media != null ? media : Collections.emptyList()
            );

            case SYSTEM:
            return SystemMessage.builder()
                .text(text)
                .metadata(metadata)
                .build();

            case TOOL:
            return new ToolResponseMessage(
                    toolResponses != null ? toolResponses : Collections.emptyList(),
            metadata
            );

            default:
            throw new IllegalArgumentException("Unsupported message type: " + messageType);
        }
    }
}