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
        try {
            log.info("📤 Sending message to Vertex AI Gemini...");
            log.info("Message: {}", message);
            log.info("Context ID: {}", contextId);

            ResponseEntity<ChatResponse, CrawlerResult> response = crawlerChatClient
                    .prompt()
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, contextId))
                    .call()
                    .responseEntity(CrawlerResult.class);

            log.info("✅ Received response from Vertex AI");
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to call Vertex AI Gemini", e);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());

            if (e.getCause() != null) {
                log.error("Caused by: {}", e.getCause().getClass().getName());
                log.error("Cause message: {}", e.getCause().getMessage());
            }

            throw new RuntimeException("Failed to call Vertex AI: " + e.getMessage(), e);
        }
    }

    /**
     * Run the crawler agent in CLI mode (interactive).
     * This method prompts for user input when needed.
     */
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

    /**
     * Run the crawler agent in non-interactive mode (for Slack/API).
     * This method does NOT prompt for user input - it returns the result immediately.
     * If the agent needs more input, the caller should handle asking the user.
     */
    public ResponseEntity<ChatResponse, CrawlerResult> runNonInteractive(String userMessage) {
        String conversationId = UUID.randomUUID().toString();

        log.info("🔍 Crawling Agent starting (non-interactive mode)");
        ResponseEntity<ChatResponse, CrawlerResult> result = sendMessage(userMessage, conversationId);

        switch (result.entity().getFinishReason()) {
            case NEEDS_MORE_INPUT:
                log.info("🔁 Crawling Agent needs more input: {}", result.entity().getNextPrompt());
                // Store conversation ID so we can continue the conversation
                result.entity().setConversationId(conversationId);
                break;

            case COMPLETED:
                log.info("✅ Crawling Agent completed its task");
                break;

            case ERROR:
                log.error("❌ Crawling Agent failed: {}", result.getEntity().getFinalResponse());
                break;
        }

        log.info("Crawling finished for context {}.", conversationId);
        return result;
    }

    /**
     * Continue a conversation with the crawler agent (for follow-up questions).
     * Used when the agent previously returned NEEDS_MORE_INPUT.
     */
    public ResponseEntity<ChatResponse, CrawlerResult> continueConversation(String userMessage, String conversationId) {
        log.info("🔍 Crawling Agent continuing conversation: {}", conversationId);
        return sendMessage(userMessage, conversationId);
    }
}