package com.redis.socialmediatracker.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/slack/oauth")
public class SlackOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(SlackOAuthController.class);

    private final SlackTokenRepository slackTokenRepository;

    @Value("${slack.client.id}")
    private String clientId;

    @Value("${slack.client.secret}")
    private String clientSecret;

    public SlackOAuthController(SlackTokenRepository slackTokenRepository) {
        this.slackTokenRepository = slackTokenRepository;
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        try {
            WebClient webClient = WebClient.create("https://slack.com/api/oauth.v2.access");

            Map<String, Object> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("client_id", clientId)
                            .queryParam("client_secret", clientSecret)
                            .queryParam("code", code)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                logger.error("OAuth failed: {}", response);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("OAuth authorization failed. Please try again.");
            }

            // Extract team information
            Map<String, Object> team = (Map<String, Object>) response.get("team");
            String teamId = (String) team.get("id");
            String teamName = (String) team.get("name");

            // Extract bot token
            Map<String, Object> authedUser = (Map<String, Object>) response.get("authed_user");
            String userAccessToken = authedUser != null ? (String) authedUser.get("access_token") : null;

            // Extract bot information
            String botUserId = (String) response.get("bot_user_id");
            String botAccessToken = (String) response.get("access_token");
            String scope = (String) response.get("scope");

            // Create or update token in Redis
            SlackToken slackToken = slackTokenRepository.findByTeamId(teamId)
                    .orElse(new SlackToken());

            slackToken.setTeamId(teamId);
            slackToken.setTeamName(teamName);
            slackToken.setBotUserId(botUserId);
            slackToken.setBotAccessToken(botAccessToken);
            slackToken.setUserAccessToken(userAccessToken);
            slackToken.setScope(scope);

            slackTokenRepository.save(slackToken);

            logger.info("✅ Successfully stored Slack token for team: {} ({})", teamName, teamId);

            return ResponseEntity.ok(
                    "Successfully connected to Slack workspace: " + teamName +
                    "! You can now interact with the bot in your Slack channels."
            );

        } catch (Exception e) {
            logger.error("❌ Error during OAuth callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during authorization. Please try again.");
        }
    }
}