package com.redis.socialmediatracker.agent.memory;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.socialmediatracker.agent.analysisagent.AnalysisResult;
import com.redis.socialmediatracker.agent.collectoragent.CrawlerResult;
import com.redis.socialmediatracker.agent.insightsagent.InsightResult;
import org.springframework.data.annotation.Id;

/**
 * Represents the state of an ongoing conversation with the agent pipeline.
 * Used to track multi-turn conversations when agents need more input.
 * <p>
 * Stored in Redis using Redis OM Spring for persistence across container restarts.
 */
@Document(value = "conversation-state", indexName = "conversationStateIdx", timeToLive = 1800)
public class ConversationState {

    public enum Stage {
        CRAWLER,
        ANALYSIS,
        INSIGHT,
        REPORT,
        COMPLETED
    }

    @Id
    private String id;

    @Indexed
    private String conversationId;

    @Indexed
    private Stage currentStage;

    @Indexed
    private String teamId;

    @Indexed
    private String channel;

    @Indexed
    private String threadTs;

    @Indexed
    private long lastActivityTimestamp;

    @Indexed
    private CrawlerResult crawlerResultJson;

    @Indexed
    private AnalysisResult analysisResultJson;

    @Indexed
    private InsightResult insightResultJson;

    @Indexed
    private com.redis.socialmediatracker.agent.reportagent.ReportResult reportResultJson;

    /**
     * Default constructor required by Redis OM Spring.
     */
    public ConversationState() {
        this.currentStage = Stage.CRAWLER;
        this.lastActivityTimestamp = System.currentTimeMillis();
    }

    /**
     * Constructor for creating a new conversation state.
     */
    public ConversationState(String conversationId, String teamId, String channel, String threadTs) {
        this.id = createKey(teamId, channel, threadTs);
        this.conversationId = conversationId;
        this.teamId = teamId;
        this.channel = channel;
        this.threadTs = threadTs;
        this.currentStage = Stage.CRAWLER;
        this.lastActivityTimestamp = System.currentTimeMillis();
    }

    /**
     * Create a composite key for Redis storage.
     */
    private static String createKey(String teamId, String channel, String threadTs) {
        return teamId + ":" + channel + ":" + (threadTs != null ? threadTs : "");
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public CrawlerResult getCrawlerResult() {
        return crawlerResultJson;
    }

    public void setCrawlerResult(CrawlerResult crawlerResultJson) {
        this.crawlerResultJson = crawlerResultJson;
    }

    public AnalysisResult getAnalysisResult() {
        return analysisResultJson;
    }

    public void setAnalysisResult(AnalysisResult analysisResultJson) {
        this.analysisResultJson = analysisResultJson;
    }

    public InsightResult getInsightResult() {
        return insightResultJson;
    }

    public void setInsightResult(InsightResult insightResultJson) {
        this.insightResultJson = insightResultJson;
    }

    public com.redis.socialmediatracker.agent.reportagent.ReportResult getReportResult() {
        return reportResultJson;
    }

    public void setReportResult(com.redis.socialmediatracker.agent.reportagent.ReportResult reportResultJson) {
        this.reportResultJson = reportResultJson;
    }

    /**
     * Calculate total tokens consumed across all agent stages.
     * Reads token counts from nested result objects.
     *
     * @return Total tokens consumed, or 0 if no results available
     */
    public Long getTotalTokens() {
        long total = 0L;

        if (crawlerResultJson != null && crawlerResultJson.getTokens() != null) {
            total += crawlerResultJson.getTokens();
        }
        if (analysisResultJson != null && analysisResultJson.getTokens() != null) {
            total += analysisResultJson.getTokens();
        }
        if (insightResultJson != null && insightResultJson.getTokens() != null) {
            total += insightResultJson.getTokens();
        }
        if (reportResultJson != null && reportResultJson.getTokens() != null) {
            total += reportResultJson.getTokens();
        }

        return total;
    }

    public void updateActivity() {
        this.lastActivityTimestamp = System.currentTimeMillis();
    }
}

