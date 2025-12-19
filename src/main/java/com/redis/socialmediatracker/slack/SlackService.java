package com.redis.socialmediatracker.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with Slack API.
 * Handles sending messages, posting in threads, and other Slack operations.
 */
@Service
public class SlackService {

    private static final Logger logger = LoggerFactory.getLogger(SlackService.class);

    private final SlackTokenRepository slackTokenRepository;
    private final WebClient webClient;

    @Value("${slack.bot.token:}")
    private String defaultBotToken;

    public SlackService(SlackTokenRepository slackTokenRepository) {
        this.slackTokenRepository = slackTokenRepository;
        this.webClient = WebClient.builder()
                .baseUrl("https://slack.com/api")
                .build();
    }

    /**
     * Send a message to a Slack channel.
     * 
     * @param teamId The Slack team ID
     * @param channel The channel ID to send to
     * @param text The message text
     * @return The timestamp of the sent message (for threading)
     */
    public String sendMessage(String teamId, String channel, String text) {
        return sendMessage(teamId, channel, text, null);
    }

    /**
     * Send a message to a Slack channel, optionally in a thread.
     * 
     * @param teamId The Slack team ID
     * @param channel The channel ID to send to
     * @param text The message text
     * @param threadTs The thread timestamp (null for new message)
     * @return The timestamp of the sent message
     */
    public String sendMessage(String teamId, String channel, String text, String threadTs) {
        String botToken = getBotToken(teamId);
        if (botToken == null) {
            logger.error("No bot token found for team: {}", teamId);
            return null;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel);
        payload.put("text", text);
        
        if (threadTs != null) {
            payload.put("thread_ts", threadTs);
        }

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat.postMessage")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                String ts = (String) response.get("ts");
                logger.info("✅ Message sent to channel {} (thread: {})", channel, threadTs != null ? threadTs : "none");
                return ts;
            } else {
                logger.error("❌ Failed to send message: {}", response);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ Error sending message to Slack: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Send a formatted markdown message to Slack.
     * 
     * @param teamId The Slack team ID
     * @param channel The channel ID
     * @param markdown The markdown text
     * @param threadTs Optional thread timestamp
     * @return The timestamp of the sent message
     */
    public String sendMarkdownMessage(String teamId, String channel, String markdown, String threadTs) {
        String botToken = getBotToken(teamId);
        if (botToken == null) {
            logger.error("No bot token found for team: {}", teamId);
            return null;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel);
        payload.put("text", markdown);
        payload.put("mrkdwn", true);
        
        if (threadTs != null) {
            payload.put("thread_ts", threadTs);
        }

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat.postMessage")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                return (String) response.get("ts");
            } else {
                logger.error("❌ Failed to send markdown message: {}", response);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ Error sending markdown message: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get the bot token for a specific team.
     */
    private String getBotToken(String teamId) {
        if (teamId == null || teamId.isEmpty()) {
            return defaultBotToken.isEmpty() ? null : defaultBotToken;
        }
        
        return slackTokenRepository.findByTeamId(teamId)
                .map(SlackToken::getBotAccessToken)
                .orElse(defaultBotToken.isEmpty() ? null : defaultBotToken);
    }
}

