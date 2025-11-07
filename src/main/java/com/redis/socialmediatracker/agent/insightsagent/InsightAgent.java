package com.redis.socialmediatracker.agent.insightsagent;

import com.redis.socialmediatracker.agent.analysisagent.AnalysisResult;
import com.redis.socialmediatracker.agent.collectoragent.CrawlerResult;
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
public class InsightAgent {

    private static final Logger log = LoggerFactory.getLogger(InsightAgent.class);

    private final ChatClient insightChatClient;

    public InsightAgent(ChatClient insightChatClient) {
        this.insightChatClient = insightChatClient;
    }

    public ResponseEntity<ChatResponse, InsightResult> sendMessage(
            CrawlerResult crawlerResult,
            AnalysisResult analysisResult,
            String contextId) {
        return insightChatClient
                .prompt()
                .user(crawlerResult.getFinalResponse().toString())
                .user(analysisResult.getTopics().toString())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, contextId))
                .call()
                .responseEntity(InsightResult.class);
    }

    public ResponseEntity<ChatResponse, InsightResult> run(
            CrawlerResult crawlerResult,
            AnalysisResult analysisResult
    ) {
        Scanner scanner = new Scanner(System.in);
        String conversationId = UUID.randomUUID().toString();

        ResponseEntity<ChatResponse, InsightResult> result = null;
        boolean conversationActive = true;

        while (conversationActive) {
            result = sendMessage(crawlerResult, analysisResult, conversationId);

            switch (result.entity().getFinishReason()) {
                case COMPLETED -> {
                    log.info("✅ Insight Agent completed its task:");
                }
                case ERROR -> {
                    log.error("❌ Insight Agent failed: {}", result.getEntity().getFinishReason());
                }
            }
            conversationActive = false;
        }

        scanner.close();
        log.info("Insight generation finished for context {}.", conversationId);
        return result;
    }
}