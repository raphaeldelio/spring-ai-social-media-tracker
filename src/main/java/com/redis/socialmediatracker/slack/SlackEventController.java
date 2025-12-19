package com.redis.socialmediatracker.slack;

import com.redis.socialmediatracker.agent.AgentOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling Slack Events API callbacks.
 * Receives events like app mentions, direct messages, etc.
 */
@RestController
@RequestMapping("/slack/events")
public class SlackEventController {

    private static final Logger logger = LoggerFactory.getLogger(SlackEventController.class);

    private final AgentOrchestrationService agentOrchestrationService;
    private final SlackService slackService;

    @Value("${slack.signing.secret}")
    private String signingSecret;

    public SlackEventController(
            AgentOrchestrationService agentOrchestrationService,
            SlackService slackService) {
        this.agentOrchestrationService = agentOrchestrationService;
        this.slackService = slackService;
    }

    /**
     * Handle incoming Slack events.
     * This endpoint receives all events configured in your Slack app.
     */
    @PostMapping
    public ResponseEntity<?> handleEvent(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Slack-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Slack-Signature", required = false) String signature) {

        // Handle URL verification challenge (first-time setup)
        if ("url_verification".equals(payload.get("type"))) {
            logger.info("Handling URL verification challenge");
            return ResponseEntity.ok(Map.of("challenge", payload.get("challenge")));
        }

        // Verify request signature (security)
        // Note: In production, you should verify the signature
        // For now, we'll log it but not enforce it during development
        if (timestamp != null && signature != null) {
            // TODO: Implement signature verification for production
            logger.debug("Received signed request from Slack");
        }

        // Handle event callbacks
        if ("event_callback".equals(payload.get("type"))) {
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            String eventType = (String) event.get("type");

            logger.info("Received Slack event: {}", eventType);

            // Handle app mentions (@bot_name)
            if ("app_mention".equals(eventType)) {
                handleAppMention(event);
            }
            // Handle direct messages
            else if ("message".equals(eventType)) {
                handleMessage(event);
            }

            // Return 200 immediately to acknowledge receipt
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unknown event type");
    }

    /**
     * Handle when the bot is mentioned in a channel.
     */
    private void handleAppMention(Map<String, Object> event) {
        String text = (String) event.get("text");
        String channel = (String) event.get("channel");
        String user = (String) event.get("user");
        String ts = (String) event.get("ts");
        String teamId = (String) event.get("team");

        logger.info("App mentioned in channel {} by user {}: {}", channel, user, text);

        // Remove the bot mention from the text
        String cleanedText = text.replaceAll("<@[A-Z0-9]+>", "").trim();

        if (cleanedText.isEmpty()) {
            slackService.sendMessage(
                    teamId,
                    channel,
                    "Hi! 👋 I can help you analyze social media trends. Try asking me something like:\n" +
                    "• _Search for posts about Redis_\n" +
                    "• _What are people saying about Redis on social media?_\n" +
                    "• _Analyze recent Redis discussions_",
                    ts
            );
            return;
        }

        // Acknowledge the request
        slackService.sendMessage(
                teamId,
                channel,
                "Got it! I'll analyze that for you. This may take a minute... ⏳",
                ts
        );

        // Process the request asynchronously
        agentOrchestrationService.processRequest(teamId, channel, ts, cleanedText);
    }

    /**
     * Handle direct messages to the bot and thread replies.
     */
    private void handleMessage(Map<String, Object> event) {
        // Ignore bot messages and message changes
        if (event.containsKey("bot_id") || event.containsKey("subtype")) {
            return;
        }

        String text = (String) event.get("text");
        String channel = (String) event.get("channel");
        String user = (String) event.get("user");
        String ts = (String) event.get("ts");
        String teamId = (String) event.get("team");
        String channelType = (String) event.get("channel_type");
        String threadTs = (String) event.get("thread_ts");

        // Handle direct messages (DMs)
        if ("im".equals(channelType)) {
            logger.info("Direct message from user {}: {}", user, text);

            if (text == null || text.trim().isEmpty()) {
                return;
            }

            // Acknowledge the request
            slackService.sendMessage(
                    teamId,
                    channel,
                    "Got it! I'll analyze that for you. This may take a minute... ⏳",
                    ts
            );

            // Process the request asynchronously
            agentOrchestrationService.processRequest(teamId, channel, ts, text);
            return;
        }

        // Handle thread replies in channels
        if (threadTs != null) {
            logger.info("Thread reply in channel {} by user {}: {}", channel, user, text);

            if (text == null || text.trim().isEmpty()) {
                return;
            }

            // Process the thread reply (continue conversation)
            agentOrchestrationService.processRequest(teamId, channel, threadTs, text);
        }
    }
}

