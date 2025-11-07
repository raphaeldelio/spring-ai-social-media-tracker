package com.redis.socialmediatracker.bluesky;

import com.redis.socialmediatracker.bluesky.model.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Service responsible for querying the Bluesky API for posts.
 * Designed with clean, maintainable and testable code – no Lombok used.
 */
@Service
public class BlueskyService {

    private static final Logger logger = LoggerFactory.getLogger(BlueskyService.class);

    private final AuthenticationService authenticationService;
    private final RestTemplate restTemplate;
    private final String apiUrl;

    private String token;
    private LocalDateTime lastTokenUpdate;

    public BlueskyService(
            AuthenticationService authenticationService, RestTemplate restTemplate,
            @Value("${bluesky.api.url}") String apiUrl
    ) {
        this.authenticationService = authenticationService;
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    /**
     * Searches for posts containing the given tag since the specified date.
     *
     * @param tag   Search tag
     * @param since Starting date for filtering posts
     * @param limit Number of posts per API page
     * @return List of fetched posts
     */
    public List<Post> searchPosts(String tag, OffsetDateTime since, int limit) {
        Objects.requireNonNull(tag, "Tag cannot be null");
        Objects.requireNonNull(since, "Since date cannot be null");

        List<Post> allPosts = new ArrayList<>();
        String cursor = null;

        logger.info("🔍 Searching posts with tag '{}' since {}", tag, since);

        while (true) {
            ResponseEntity<Map> response = makeRequest(cursor, limit, since, tag);

            if (response == null || response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                logger.warn("⚠️ Failed to fetch posts. Status: {}", 
                        response != null ? response.getStatusCode() : "NO RESPONSE");
                break;
            }

            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> postsData = 
                    (List<Map<String, Object>>) body.getOrDefault("posts", Collections.emptyList());

            for (Map<String, Object> data : postsData) {
                allPosts.add(Post.fromMap(data));
            }

            logger.info("✅ Retrieved {} posts. Total so far: {}", postsData.size(), allPosts.size());

            cursor = (String) body.get("cursor");
            if (cursor == null || cursor.isBlank()) {
                break;
            }
        }

        logger.info("🎉 Completed fetching posts for tag '{}'. Total retrieved: {}", tag, allPosts.size());
        return Collections.unmodifiableList(allPosts);
    }

    /**
     * Builds and executes the HTTP GET request for Bluesky API.
     */
    private ResponseEntity<Map> makeRequest(String cursor, int limit, OffsetDateTime since, String tag) {
        if (token == null || lastTokenUpdate == null || lastTokenUpdate.isBefore(LocalDateTime.now().minusMinutes(15))) {
            token = authenticationService.getAccessToken();
        }

        String endpoint = apiUrl + "/app.bsky.feed.searchPosts";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", tag);
        params.put("sort", "latest");
        params.put("limit", String.valueOf(limit));
        params.put("since", since.withOffsetSameInstant(ZoneOffset.UTC).toString());
        if (cursor != null) params.put("cursor", cursor);

        try {
            URI uri = new URI(endpoint + buildQuery(params));
            HttpEntity<Void> request = new HttpEntity<>(headers);
            return restTemplate.exchange(uri, HttpMethod.GET, request, Map.class);
        } catch (URISyntaxException | RestClientException e) {
            logger.error("❌ Request error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts parameters into an encoded query string.
     */
    private String buildQuery(Map<String, String> params) {
        if (params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(entry.getKey())
              .append("=")
              .append(entry.getValue())
              .append("&");
        }
        sb.setLength(sb.length() - 1); // remove trailing '&'
        return sb.toString();
    }
}