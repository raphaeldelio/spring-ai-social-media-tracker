package com.redis.socialmediatracker.agent.collectoragent;

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
public class CrawlerAgent {

    private static final Logger log = LoggerFactory.getLogger(CrawlerAgent.class);

    private final ChatClient crawlerChatClient;

    public CrawlerAgent(ChatClient crawlerChatClient) {
        this.crawlerChatClient = crawlerChatClient;
    }

    private ResponseEntity<ChatResponse, CrawlerResult> sendMessage(String message, String contextId) {
        return crawlerChatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, contextId))
                .call()
                .responseEntity(CrawlerResult.class);
    }

    public ResponseEntity<ChatResponse, CrawlerResult> run(String userMessage) {
        Scanner scanner = new Scanner(System.in);
        String conversationId = UUID.randomUUID().toString();

        ResponseEntity<ChatResponse, CrawlerResult> result = null;
        boolean conversationActive = true;

        while (conversationActive) {
            result = sendMessage(userMessage, conversationId);

            switch (result.entity().getFinishReason()) {
                case NEEDS_MORE_INPUT:
                    log.info("🔁 Crawling Agent needs more input:");
                    log.info(result.getEntity().getNextPrompt());

                    // Get user input or forward automatically
                    System.out.print("You: ");
                    userMessage = scanner.nextLine();
                    break;

                case COMPLETED:
                    log.info("✅ Crawling Agent completed its task:");
                    conversationActive = false;
                    break;

                case ERROR:
                    log.error("❌ Crawling Agent failed: {}", result.getEntity().getFinalResponse());
                    conversationActive = false;
                    break;
            }
        }

        scanner.close();
        log.info("Crawling finished for context {}.", conversationId);
        return result;
    }
}