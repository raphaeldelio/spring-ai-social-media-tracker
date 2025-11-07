package com.redis.socialmediatracker.agent.reportagent;

import com.redis.socialmediatracker.agent.analysisagent.AnalysisResult;
import com.redis.socialmediatracker.agent.collectoragent.CrawlerResult;
import com.redis.socialmediatracker.agent.insightsagent.InsightResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.Scanner;
import java.util.UUID;

@Service
public class ReportAgent {

    private static final Logger log = LoggerFactory.getLogger(ReportAgent.class);

    private final ChatClient insightChatClient;

    public ReportAgent(ChatClient insightChatClient) {
        this.insightChatClient = insightChatClient;
    }

    public ResponseEntity<ChatResponse, ReportResult> sendMessage(
            CrawlerResult crawlerResult,
            AnalysisResult analysisResult,
            InsightResult insightResult,
            String contextId) {
        return insightChatClient
                .prompt()
                .user(crawlerResult.getFinalResponse().toString())
                .user(analysisResult.getTopics().toString())
                .user(insightResult.getInsights().toString())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, contextId))
                .call()
                .responseEntity(ReportResult.class);
    }

    public ResponseEntity<ChatResponse, ReportResult> run(
            CrawlerResult crawlerResult,
            AnalysisResult analysisResult,
            InsightResult insightResult
    ) {
        Scanner scanner = new Scanner(System.in);
        String conversationId = UUID.randomUUID().toString();

        ResponseEntity<ChatResponse, ReportResult> result = null;
        boolean conversationActive = true;

        while (conversationActive) {
            result = sendMessage(crawlerResult, analysisResult, insightResult, conversationId);

            switch (result.entity().getFinishReason()) {
                case COMPLETED -> {
                    log.info("✅ Report Agent completed its task:");
                }
                case ERROR -> {
                    log.error("❌ Report Agent failed: {}", result.getEntity().getFinishReason());
                }
            }
            conversationActive = false;
        }

        scanner.close();
        log.info("Report generation finished for context {}.", conversationId);
        return result;
    }
}