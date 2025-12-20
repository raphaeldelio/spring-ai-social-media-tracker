package com.redis.socialmediatracker.agent.collectoragent;

import com.redis.om.spring.annotations.Indexed;

public class CrawlerResult {

    public enum FinishReason {
        NEEDS_MORE_INPUT,
        COMPLETED,
        ERROR
    }

    @Indexed
    private FinishReason finishReason;

    @Indexed
    private String nextPrompt;

    @Indexed
    private String conversationId;

    @Indexed
    private FetchedDataResponse finalResponse;

    public CrawlerResult() {}

    public CrawlerResult(FinishReason finishReason, String nextPrompt, FetchedDataResponse finalResponse) {
        this.finishReason = finishReason;
        this.nextPrompt = nextPrompt;
        this.finalResponse = finalResponse;
    }

    // Static factory methods for clarity
    public static CrawlerResult needsMoreInput(String nextPrompt) {
        return new CrawlerResult(FinishReason.NEEDS_MORE_INPUT, nextPrompt, null);
    }

    public static CrawlerResult completed(FetchedDataResponse finalResponse) {
        return new CrawlerResult(FinishReason.COMPLETED, null, finalResponse);
    }

    public static CrawlerResult error(FetchedDataResponse message) {
        return new CrawlerResult(FinishReason.ERROR, null, message);
    }

    // Getters and setters
    public FinishReason getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(FinishReason finishReason) {
        this.finishReason = finishReason;
    }

    public String getNextPrompt() {
        return nextPrompt;
    }

    public void setNextPrompt(String nextPrompt) {
        this.nextPrompt = nextPrompt;
    }

    public FetchedDataResponse getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(FetchedDataResponse finalResponse) {
        this.finalResponse = finalResponse;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}