package com.redis.socialmediatracker.slack;

import com.redis.socialmediatracker.agent.AgentOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling Slack Events API callbacks.
 * Receives events like app mentions, direct messages, etc.
 *
 * Security: All requests are verified using Slack's signature verification
 * to ensure they originate from Slack and haven't been tampered with.
 */
@RestController
@RequestMapping("/slack/events")
public class SlackEventController {

    private static final Logger logger = LoggerFactory.getLogger(SlackEventController.class);

    private final AgentOrchestrationService agentOrchestrationService;
    private final SlackService slackService;
    private final SlackSignatureVerifier signatureVerifier;
    private final EventDeduplicationService eventDeduplicationService;

    public SlackEventController(
            AgentOrchestrationService agentOrchestrationService,
            SlackService slackService,
            SlackSignatureVerifier signatureVerifier,
            EventDeduplicationService eventDeduplicationService) {
        this.agentOrchestrationService = agentOrchestrationService;
        this.slackService = slackService;
        this.signatureVerifier = signatureVerifier;
        this.eventDeduplicationService = eventDeduplicationService;
    }

    /**
     * Handle incoming Slack events.
     * This endpoint receives all events configured in your Slack app.
     *
     * Security: Verifies Slack signature before processing any events.
     */
    @PostMapping
    public ResponseEntity<?> handleEvent(
            HttpServletRequest request,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Slack-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Slack-Signature", required = false) String signature) {

        // Handle URL verification challenge (first-time setup)
        // This happens when you first configure the Events API URL in Slack
        if ("url_verification".equals(payload.get("type"))) {
            logger.info("Handling URL verification challenge");
            return ResponseEntity.ok(Map.of("challenge", payload.get("challenge")));
        }

        // Verify request signature (SECURITY CRITICAL)
        // Get the raw request body from the cached request
        String requestBody = null;
        if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
            requestBody = cachedRequest.getBody();
        }

        if (requestBody == null) {
            logger.error("Unable to retrieve request body for signature verification");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Verify the signature
        if (!signatureVerifier.verifySignature(timestamp, signature, requestBody)) {
            logger.warn("⚠️ SECURITY: Invalid Slack signature detected! Rejecting request.");
            logger.warn("Timestamp: {}, Signature: {}", timestamp, signature);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
        }

        logger.debug("✅ Slack signature verified successfully");

        // Handle event callbacks
        if ("event_callback".equals(payload.get("type"))) {
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            String eventType = (String) event.get("type");
            String teamId = (String) payload.get("team_id");

            String eventId = (String) payload.get("event_id");
            if (!eventDeduplicationService.isNewEvent(eventId, eventType, teamId)) {
                logger.info("⏭️ Skipping duplicate event: {} (type: {})", eventId, eventType);
                // Return 200 to acknowledge - we've already processed this
                return ResponseEntity.ok().build();
            }

            logger.info("Received Slack event: {} (id: {})", eventType, eventId);

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

        agentOrchestrationService.processRequest(teamId, channel, ts, cleanedText);
    }

    /**
     * Handle direct messages to the bot and thread replies.
     */
    private void handleMessage(Map<String, Object> event) {
        // Ignore bot messages and message changes
        // Check for bot_id, bot_profile, or subtype to filter out bot messages
        if (event.containsKey("bot_id") ||
            event.containsKey("bot_profile") ||
            event.containsKey("subtype")) {
            logger.debug("Ignoring bot message or message with subtype");
            return;
        }

        String text = (String) event.get("text");
        String channel = (String) event.get("channel");
        String user = (String) event.get("user");
        String ts = (String) event.get("ts");
        String teamId = (String) event.get("team");
        String channelType = (String) event.get("channel_type");
        String threadTs = (String) event.get("thread_ts");

        // Additional safety: ignore if no user (bot messages sometimes don't have bot_id)
        if (user == null || user.trim().isEmpty()) {
            logger.debug("Ignoring message with no user");
            return;
        }

        // Handle direct messages (DMs)
        if ("im".equals(channelType)) {
            logger.info("Direct message from user {}: {}", user, text);

            if (text == null || text.trim().isEmpty()) {
                return;
            }

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

