package com.redis.socialmediatracker.agent.analysisagent;

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
public class AnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(AnalysisAgent.class);

    private final ChatClient analysisChatClient;

    public AnalysisAgent(ChatClient analysisChatClient) {
        this.analysisChatClient = analysisChatClient;
    }

    public ResponseEntity<ChatResponse, AnalysisResult> sendMessage(CrawlerResult crawlerResult, String contextId) {
        return analysisChatClient
                .prompt()
                .user(crawlerResult.getFinalResponse().toString())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, contextId))
                .call()
                .responseEntity(AnalysisResult.class);
    }

    public ResponseEntity<ChatResponse, AnalysisResult> run(CrawlerResult crawlerResult) {
        Scanner scanner = new Scanner(System.in);
        String conversationId = UUID.randomUUID().toString();

        ResponseEntity<ChatResponse, AnalysisResult> result = null;
        boolean conversationActive = true;

        while (conversationActive) {
            result = sendMessage(crawlerResult, conversationId);

            switch (result.entity().getFinishReason()) {
                case COMPLETED -> {
                    log.info("✅ Analysis Agent completed its task:");
                }
                case ERROR -> {
                    log.error("❌ Analysis Agent failed: {}", result.getEntity().getFinishReason());
                }
            }
            conversationActive = false;
        }

        scanner.close();
        log.info("Analysis finished for context {}.", conversationId);
        return result;
    }
}