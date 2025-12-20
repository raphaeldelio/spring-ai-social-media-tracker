package com.redis.socialmediatracker.agent;

import com.redis.socialmediatracker.agent.analysisagent.AnalysisAgent;
import com.redis.socialmediatracker.agent.collectoragent.CrawlerAgent;
import com.redis.socialmediatracker.agent.collectoragent.CrawlerResult;
import com.redis.socialmediatracker.agent.insightsagent.InsightAgent;
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
        ConversationState state = null;
        try {
            logger.info("🚀 Starting agent pipeline for message: {}", userMessage);

            // Get or create conversation state
            state = conversationStateManager.getOrCreateConversation(teamId, channel, threadTs);

            // Mark as running
            state.setRunning(true);
            conversationStateManager.updateConversation(state);

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

            // Mark as not running on error
            if (state != null) {
                state.setRunning(false);
                conversationStateManager.updateConversation(state);
            }

            slackService.sendMessage(
                    teamId,
                    channel,
                    "❌ An error occurred while processing your request: " + e.getMessage(),
                    threadTs
            );
        }
    }

    /**
     * Resume a conversation from its current stage.
     * Used for startup recovery to continue interrupted workflows.
     *
     * @param state The conversation state to resume
     */
    @Async
    public void resumeConversation(ConversationState state) {
        try {
            logger.info("🔄 Resuming conversation {} from stage {}",
                state.getConversationId(), state.getCurrentStage());

            slackService.sendMessage(
                state.getTeamId(),
                state.getChannel(),
                "🔄 Application restarted. Resuming your request from where we left off...",
                state.getThreadTs()
            );

            // Mark as running
            state.setRunning(true);
            conversationStateManager.updateConversation(state);

            // Resume from current stage
            if (state.getCurrentStage() == ConversationState.Stage.ANALYSIS) {
                processAnalysisStage(state);
            } else if (state.getCurrentStage() == ConversationState.Stage.INSIGHT) {
                processInsightStage(state);
            } else if (state.getCurrentStage() == ConversationState.Stage.REPORT) {
                processReportStage(state);
            } else {
                logger.warn("⚠️ Cannot resume from stage {}, marking as not running", state.getCurrentStage());
                state.setRunning(false);
                conversationStateManager.updateConversation(state);
            }

        } catch (Exception e) {
            logger.error("❌ Error resuming conversation: {}", e.getMessage(), e);

            // Mark as not running on error
            state.setRunning(false);
            conversationStateManager.updateConversation(state);

            slackService.sendMessage(
                state.getTeamId(),
                state.getChannel(),
                "❌ An error occurred while resuming your request: " + e.getMessage(),
                state.getThreadTs()
            );
        }
    }

    private void processCrawlerStage(ConversationState state, String userMessage) {
        // Step 1: Crawler Agent - Fetch data
        slackService.sendMessage(state.getTeamId(), state.getChannel(), "🔍 Crawling Agent is working...", state.getThreadTs());

        ResponseEntity<ChatResponse, CrawlerResult> crawlerResult;
        try {
            crawlerResult = state.getCrawlerResult() != null && state.getConversationId() != null
                    ? crawlerAgent.continueConversation(userMessage, state.getConversationId())
                    : crawlerAgent.runNonInteractive(userMessage);
        } catch (Exception e) {
            logger.error("❌ Crawler agent failed", e);
            slackService.sendMessage(state.getTeamId(), state.getChannel(),
                    "❌ Failed to connect to AI service. Please check the logs and try again.\n\n" +
                    "Error: " + e.getMessage(),
                    state.getThreadTs());
            return;
        }

        if (crawlerResult == null || crawlerResult.entity() == null) {
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❌ Failed to fetch data. Please try again.", state.getThreadTs());
            return;
        }

        // Check if crawler needs more input
        if (crawlerResult.entity().getFinishReason() == CrawlerResult.FinishReason.NEEDS_MORE_INPUT) {
            logger.info("🔁 Crawler needs more input, asking user in Slack");
            String question = crawlerResult.entity().getNextPrompt();
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❓ " + question, state.getThreadTs());

            // Store the conversation ID so we can continue
            state.setConversationId(crawlerResult.entity().getConversationId());
            state.setCrawlerResult(crawlerResult.entity());

            // Mark as not running - waiting for user input
            state.setRunning(false);
            conversationStateManager.updateConversation(state);
            return;
        }

        logger.info("✅ Crawler completed");

        // Track token usage in the result object
        Long crawlerTokens = extractTokenUsage(crawlerResult.response());
        crawlerResult.entity().setTokens(crawlerTokens);
        logger.info("📊 Crawler used {} tokens", crawlerTokens);

        slackService.sendMessage(state.getTeamId(), state.getChannel(),
            "🔍 Crawling Agent found " + crawlerResult.entity().getFinalResponse().getFetchedData().size() +
            " posts. Sending them to the Analysis Agent. (Tokens: " + crawlerTokens + ")",
            state.getThreadTs());

        // Move to next stage
        state.setCrawlerResult(crawlerResult.entity());
        state.setCurrentStage(ConversationState.Stage.ANALYSIS);
        conversationStateManager.updateConversation(state);

        // Continue to analysis
        processAnalysisStage(state);
    }

    private void processAnalysisStage(ConversationState state) {
        var crawlerResult = state.getCrawlerResult();

        // Step 2: Analysis Agent - Analyze topics
        slackService.sendMessage(state.getTeamId(), state.getChannel(), "📊 Analysis Agent is analyzing topics and trends...", state.getThreadTs());
        var analysisResult = analysisAgent.runNonInteractive(crawlerResult);

        if (analysisResult == null || analysisResult.entity() == null) {
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❌ Failed to analyze data. Please try again.", state.getThreadTs());
            return;
        }

        logger.info("✅ Analysis completed");

        // Track token usage in the result object
        Long analysisTokens = extractTokenUsage(analysisResult.response());
        analysisResult.entity().setTokens(analysisTokens);
        logger.info("📊 Analysis used {} tokens", analysisTokens);

        slackService.sendMessage(state.getTeamId(), state.getChannel(),
            "📊 Analysis Agent completed its task. Sending results to the Insights Agent. (Tokens: " + analysisTokens + ")",
            state.getThreadTs());

        // Move to next stage
        state.setAnalysisResult(analysisResult.entity());
        state.setCurrentStage(ConversationState.Stage.INSIGHT);
        conversationStateManager.updateConversation(state);

        // Continue to insights
        processInsightStage(state);
    }

    private void processInsightStage(ConversationState state) {
        var crawlerResult = state.getCrawlerResult();
        var analysisResult = state.getAnalysisResult();

        // Step 3: Insight Agent - Generate insights
        slackService.sendMessage(state.getTeamId(), state.getChannel(), "💡 Insights Agent is generating insights...", state.getThreadTs());
        var insightResult = insightAgent.runNonInteractive(crawlerResult, analysisResult);

        if (insightResult == null || insightResult.entity() == null) {
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❌ Failed to generate insights. Please try again.", state.getThreadTs());
            return;
        }

        logger.info("✅ Insights completed");

        // Track token usage in the result object
        Long insightTokens = extractTokenUsage(insightResult.response());
        insightResult.entity().setTokens(insightTokens);
        logger.info("📊 Insight used {} tokens", insightTokens);

        slackService.sendMessage(state.getTeamId(), state.getChannel(),
            "💡 Insights Agent completed its task. Sending results to the Report Agent. (Tokens: " + insightTokens + ")",
            state.getThreadTs());

        // Move to next stage
        state.setInsightResult(insightResult.entity());
        state.setCurrentStage(ConversationState.Stage.REPORT);
        conversationStateManager.updateConversation(state);

        // Continue to report
        processReportStage(state);
    }

    private void processReportStage(ConversationState state) {
        var crawlerResult = state.getCrawlerResult();
        var analysisResult = state.getAnalysisResult();
        var insightResult =  state.getInsightResult();

        // Step 4: Report Agent - Create final report
        slackService.sendMessage(state.getTeamId(), state.getChannel(), "📝 Report Agent is creating report...", state.getThreadTs());
        var reportResult = reportAgent.runNonInteractive(
                crawlerResult,
                analysisResult,
                insightResult
        );

        if (reportResult == null || reportResult.entity() == null) {
            slackService.sendMessage(state.getTeamId(), state.getChannel(), "❌ Failed to generate report. Please try again.", state.getThreadTs());
            return;
        }

        logger.info("✅ Report completed");

        // Track token usage in the result object
        Long reportTokens = extractTokenUsage(reportResult.response());
        reportResult.entity().setTokens(reportTokens);
        logger.info("📊 Report used {} tokens", reportTokens);

        slackService.sendMessage(state.getTeamId(), state.getChannel(),
            "📝 Report Agent finalized the report. Formatting and sending... (Tokens: " + reportTokens + ")",
            state.getThreadTs());

        // Store report result in state
        state.setReportResult(reportResult.entity());
        conversationStateManager.updateConversation(state);

        // Step 5: Send final report to Slack
        String slackFormattedReport = SlackReportFormatter.toSlackFormat(reportResult.entity());

        // Split into chunks if too long (Slack has a 4000 char limit per message)
        sendLongMessage(state.getTeamId(), state.getChannel(), state.getThreadTs(), slackFormattedReport);

        // Send token usage summary
        Long crawlerTokensSummary = state.getCrawlerResult() != null ? state.getCrawlerResult().getTokens() : null;
        Long analysisTokensSummary = state.getAnalysisResult() != null ? state.getAnalysisResult().getTokens() : null;
        Long insightTokensSummary = state.getInsightResult() != null ? state.getInsightResult().getTokens() : null;
        Long reportTokensSummary = state.getReportResult() != null ? state.getReportResult().getTokens() : null;

        String tokenSummary = String.format(
            "\n\n📊 *Token Usage Summary*\n" +
            "• Crawler: %,d tokens\n" +
            "• Analysis: %,d tokens\n" +
            "• Insights: %,d tokens\n" +
            "• Report: %,d tokens\n" +
            "• *Total: %,d tokens*",
            crawlerTokensSummary != null ? crawlerTokensSummary : 0,
            analysisTokensSummary != null ? analysisTokensSummary : 0,
            insightTokensSummary != null ? insightTokensSummary : 0,
            reportTokensSummary != null ? reportTokensSummary : 0,
            state.getTotalTokens()
        );
        slackService.sendMessage(state.getTeamId(), state.getChannel(), tokenSummary, state.getThreadTs());

        // Mark as completed and not running
        state.setCurrentStage(ConversationState.Stage.COMPLETED);
        state.setRunning(false);
        conversationStateManager.updateConversation(state);

        logger.info("🎉 Agent pipeline completed successfully. Total tokens: {}", state.getTotalTokens());
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

    /**
     * Extract total token usage from a ChatResponse.
     * Returns the total tokens (prompt + completion) or 0 if not available.
     */
    private Long extractTokenUsage(ChatResponse chatResponse) {
        if (chatResponse == null) {
            logger.warn("⚠️ ChatResponse is null, cannot extract token usage");
            return 0L;
        }

        try {
            var usage = chatResponse.getMetadata().getUsage();
            if (usage != null && usage.getTotalTokens() != null) {
                return usage.getTotalTokens().longValue();
            }
        } catch (Exception e) {
            logger.warn("⚠️ Failed to extract token usage: {}", e.getMessage());
        }

        return 0L;
    }
}

