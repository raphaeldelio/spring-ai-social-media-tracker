package com.redis.socialmediatracker.agent.memory;

/**
 * Represents the state of an ongoing conversation with the agent pipeline.
 * Used to track multi-turn conversations when agents need more input.
 */
public class ConversationState {
    
    public enum Stage {
        CRAWLER,
        ANALYSIS,
        INSIGHT,
        REPORT,
        COMPLETED
    }
    
    private String conversationId;
    private Stage currentStage;
    private String teamId;
    private String channel;
    private String threadTs;
    private long lastActivityTimestamp;
    
    // Store intermediate results
    private Object crawlerResult;
    private Object analysisResult;
    private Object insightResult;
    
    public ConversationState(String conversationId, String teamId, String channel, String threadTs) {
        this.conversationId = conversationId;
        this.teamId = teamId;
        this.channel = channel;
        this.threadTs = threadTs;
        this.currentStage = Stage.CRAWLER;
        this.lastActivityTimestamp = System.currentTimeMillis();
    }
    
    // Getters and setters
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public Stage getCurrentStage() {
        return currentStage;
    }
    
    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }
    
    public String getTeamId() {
        return teamId;
    }
    
    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public String getThreadTs() {
        return threadTs;
    }
    
    public void setThreadTs(String threadTs) {
        this.threadTs = threadTs;
    }
    
    public long getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }
    
    public void setLastActivityTimestamp(long lastActivityTimestamp) {
        this.lastActivityTimestamp = lastActivityTimestamp;
    }
    
    public Object getCrawlerResult() {
        return crawlerResult;
    }
    
    public void setCrawlerResult(Object crawlerResult) {
        this.crawlerResult = crawlerResult;
    }
    
    public Object getAnalysisResult() {
        return analysisResult;
    }
    
    public void setAnalysisResult(Object analysisResult) {
        this.analysisResult = analysisResult;
    }
    
    public Object getInsightResult() {
        return insightResult;
    }
    
    public void setInsightResult(Object insightResult) {
        this.insightResult = insightResult;
    }
    
    public void updateActivity() {
        this.lastActivityTimestamp = System.currentTimeMillis();
    }
}

