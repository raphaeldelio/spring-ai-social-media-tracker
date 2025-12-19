package com.redis.socialmediatracker.agent;

import com.redis.socialmediatracker.agent.analysisagent.AnalysisAgent;
import com.redis.socialmediatracker.agent.analysisagent.AnalysisResult;
import com.redis.socialmediatracker.agent.collectoragent.CrawlerAgent;
import com.redis.socialmediatracker.agent.collectoragent.CrawlerResult;
import com.redis.socialmediatracker.agent.insightsagent.InsightAgent;
import com.redis.socialmediatracker.agent.insightsagent.InsightResult;
import com.redis.socialmediatracker.agent.reportagent.ReportAgent;
import com.redis.socialmediatracker.agent.memory.ConversationState;
import com.redis.socialmediatracker.agent.memory.ConversationStateManager;
import com.redis.socialmediatracker.slack.SlackReportFormatter;
import com.redis.socialmediatracker.slack.SlackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service that orchestrates the multi-agent pipeline for processing user requests.
 * Runs agents sequentially: Crawler -> Analysis -> Insight -> Report
 * and sends results back to Slack.
 */
@Service
public class AgentOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrationService.class);

    private final CrawlerAgent crawlerAgent;
    private final AnalysisAgent analysisAgent;
    private final InsightAgent insightAgent;
    private final ReportAgent reportAgent;
    private final SlackService slackService;
    private final ConversationStateManager conversationStateManager;

    public AgentOrchestrationService(
            CrawlerAgent crawlerAgent,
            AnalysisAgent analysisAgent,
            InsightAgent insightAgent,
            ReportAgent reportAgent,
            SlackService slackService,
            ConversationStateManager conversationStateManager) {
        this.crawlerAgent = crawlerAgent;
        this.analysisAgent = analysisAgent;
        this.insightAgent = insightAgent;
        this.reportAgent = reportAgent;
        this.slackService = slackService;
        this.conversationStateManager = conversationStateManager;
    }

    /**
     * Process a user message through the agent pipeline asynchronously.
     * Sends progress updates to Slack as each agent completes.
     *
     * @param teamId The Slack team ID
     * @param channel The Slack channel ID
     * @param threadTs The thread timestamp to reply in
     * @param userMessage The user's request message
     */
    @Async
    public void processRequest(String teamId, String channel, String threadTs, String userMessage) {
        try {
            logger.info("🚀 Starting agent pipeline for message: {}", userMessage);

            // Get or create conversation state
            ConversationState state = conversationStateManager.getOrCreateConversation(teamId, channel, threadTs);

            // Determine which stage we're at
            if (state.getCurrentStage() == ConversationState.Stage.CRAWLER) {
                processCrawlerStage(state, userMessage);
            } else if (state.getCurrentStage() == ConversationState.Stage.ANALYSIS) {
                processAnalysisStage(state);
            } else if (state.getCurrentStage() == ConversationState.Stage.INSIGHT) {
                processInsightStage(state);
            } else if (state.getCurrentStage() == ConversationState.Stage.REPORT) {
                processReportStage(state);
            }

        } catch (Exception e) {
            logger.error("❌ Error in agent pipeline: {}", e.getMessage(), e);
            slackService.sendMessage(
                    teamId,
                    channel,
                    "❌ An error occurred while processing your request: " + e.getMessage(),
                    threadTs
            );
        }
    }

    private void processCrawlerStage(ConversationState state, String userMessage) {
        // Step 1: Crawler Agent - Fetch data
        slackService.sendMessage(state.getTeamId(), state.getChannel(), "🔍 Searching for posts...", state.getThreadTs());

        var crawlerResult = state.getCrawlerResult() != null && state.getConversationId() != null
                ? crawlerAgent.continueConversation(userMessage, state.getConversationId())
                : crawlerAgent.runNonInteractive(userMessage);

        if (crawlerResult == null || crawlerResult.entity() == null) {
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❌ Failed to fetch data. Please try again.", state.getThreadTs());
            conversationStateManager.removeConversation(state.getTeamId(), state.getChannel(), state.getThreadTs());
            return;
        }

        // Check if crawler needs more input
        if (crawlerResult.entity().getFinishReason() == CrawlerResult.FinishReason.NEEDS_MORE_INPUT) {
            logger.info("🔁 Crawler needs more input, asking user in Slack");
            String question = crawlerResult.entity().getNextPrompt();
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❓ " + question, state.getThreadTs());

            // Store the conversation ID so we can continue
            state.setConversationId(crawlerResult.entity().getConversationId());
            state.setCrawlerResult(crawlerResult);
            conversationStateManager.updateConversation(state);
            return;
        }

        logger.info("✅ Crawler completed");

        // Move to next stage
        state.setCrawlerResult(crawlerResult);
        state.setCurrentStage(ConversationState.Stage.ANALYSIS);
        conversationStateManager.updateConversation(state);

        // Continue to analysis
        processAnalysisStage(state);
    }

    private void processAnalysisStage(ConversationState state) {
        var crawlerResult = (ResponseEntity<ChatResponse, CrawlerResult>) state.getCrawlerResult();

        // Step 2: Analysis Agent - Analyze topics
        slackService.sendMessage(state.getTeamId(), state.getChannel(), "📊 Analyzing topics and trends...", state.getThreadTs());
        var analysisResult = analysisAgent.runNonInteractive(crawlerResult.entity());

        if (analysisResult == null || analysisResult.entity() == null) {
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❌ Failed to analyze data. Please try again.", state.getThreadTs());
            conversationStateManager.removeConversation(state.getTeamId(), state.getChannel(), state.getThreadTs());
            return;
        }

        logger.info("✅ Analysis completed");

        // Move to next stage
        state.setAnalysisResult(analysisResult);
        state.setCurrentStage(ConversationState.Stage.INSIGHT);
        conversationStateManager.updateConversation(state);

        // Continue to insights
        processInsightStage(state);
    }

    private void processInsightStage(ConversationState state) {
        var crawlerResult = (ResponseEntity<ChatResponse, CrawlerResult>) state.getCrawlerResult();
        var analysisResult = (ResponseEntity<ChatResponse, AnalysisResult>) state.getAnalysisResult();

        // Step 3: Insight Agent - Generate insights
        slackService.sendMessage(state.getTeamId(), state.getChannel(), "💡 Generating insights...", state.getThreadTs());
        var insightResult = insightAgent.runNonInteractive(crawlerResult.entity(), analysisResult.entity());

        if (insightResult == null || insightResult.entity() == null) {
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❌ Failed to generate insights. Please try again.", state.getThreadTs());
            conversationStateManager.removeConversation(state.getTeamId(), state.getChannel(), state.getThreadTs());
            return;
        }

        logger.info("✅ Insights completed");

        // Move to next stage
        state.setInsightResult(insightResult);
        state.setCurrentStage(ConversationState.Stage.REPORT);
        conversationStateManager.updateConversation(state);

        // Continue to report
        processReportStage(state);
    }

    private void processReportStage(ConversationState state) {
        var crawlerResult = (ResponseEntity<ChatResponse, CrawlerResult>) state.getCrawlerResult();
        var analysisResult = (ResponseEntity<ChatResponse, AnalysisResult>) state.getAnalysisResult();
        var insightResult = (ResponseEntity<ChatResponse, InsightResult>) state.getInsightResult();

        // Step 4: Report Agent - Create final report
        slackService.sendMessage(state.getTeamId(), state.getChannel(), "📝 Creating report...", state.getThreadTs());
        var reportResult = reportAgent.runNonInteractive(
                crawlerResult.entity(),
                analysisResult.entity(),
                insightResult.entity()
        );

        if (reportResult == null || reportResult.entity() == null) {
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❌ Failed to generate report. Please try again.", state.getThreadTs());
            conversationStateManager.removeConversation(state.getTeamId(), state.getChannel(), state.getThreadTs());
            return;
        }

        logger.info("✅ Report completed");

        // Step 5: Send final report to Slack
        String slackFormattedReport = SlackReportFormatter.toSlackFormat(reportResult.entity());

        // Split into chunks if too long (Slack has a 4000 char limit per message)
        sendLongMessage(state.getTeamId(), state.getChannel(), state.getThreadTs(), slackFormattedReport);

        logger.info("🎉 Agent pipeline completed successfully");

        // Clean up conversation state
        conversationStateManager.removeConversation(state.getTeamId(), state.getChannel(), state.getThreadTs());
    }

    /**
     * Send a long message by splitting it into chunks if necessary.
     */
    private void sendLongMessage(String teamId, String channel, String threadTs, String message) {
        final int MAX_LENGTH = 3900; // Leave some buffer under 4000
        
        if (message.length() <= MAX_LENGTH) {
            slackService.sendMarkdownMessage(teamId, channel, message, threadTs);
            return;
        }

        // Split by sections or paragraphs
        String[] parts = message.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();
        
        for (String part : parts) {
            if (currentChunk.length() + part.length() + 2 > MAX_LENGTH) {
                slackService.sendMarkdownMessage(teamId, channel, currentChunk.toString(), threadTs);
                currentChunk = new StringBuilder();
            }
            currentChunk.append(part).append("\n\n");
        }
        
        if (!currentChunk.isEmpty()) {
            slackService.sendMarkdownMessage(teamId, channel, currentChunk.toString(), threadTs);
        }
    }
}

