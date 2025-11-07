package com.redis.socialmediatracker.bluesky;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String username;
    private final String password;

    public AuthenticationService(
            RestTemplate restTemplate,
            @Value("${bluesky.api.url}") String apiUrl,
            @Value("${bluesky.auth.username}") String username,
            @Value("${bluesky.auth.password}") String password) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
    }

    public String getAccessToken() {
        String url = String.format("%s/com.atproto.server.createSession", apiUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = new HashMap<>();
        payload.put("identifier", username);
        payload.put("password", password);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                String did = (String) result.get("did");
                String accessJwt = (String) result.get("accessJwt");

                logger.info("✅ Login successful. DID: {}", did);
                return accessJwt != null ? accessJwt : "";
            } else {
                logger.warn("⚠️ Authentication failed: {} - {}", response.getStatusCode(), response.getBody());
                return "";
            }

        } catch (RestClientException e) {
            logger.error("❌ Request error: {}", e.getMessage());
            return "";
        }
    }
}