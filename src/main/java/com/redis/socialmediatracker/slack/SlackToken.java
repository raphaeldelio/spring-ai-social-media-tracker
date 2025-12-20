package com.redis.socialmediatracker.slack;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import org.springframework.data.annotation.Id;

import java.time.Instant;

/**
 * Entity representing a Slack workspace's OAuth tokens stored in Redis.
 * Each workspace that installs the app will have its own token record.
 */
@Document(value = "slack-token", indexName = "slackTokenIdx")
public class SlackToken {

    @Id
    private String id;

    @Indexed
    private String teamId;

    @Indexed
    private String teamName;

    private String botUserId;
    private String botAccessToken;
    private String userAccessToken;
    private String scope;
    private Instant createdAt;
    private Instant updatedAt;

    public SlackToken() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public SlackToken(String teamId, String teamName, String botUserId, 
                      String botAccessToken, String userAccessToken, String scope) {
        this();
        this.teamId = teamId;
        this.teamName = teamName;
        this.botUserId = botUserId;
        this.botAccessToken = botAccessToken;
        this.userAccessToken = userAccessToken;
        this.scope = scope;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getBotUserId() {
        return botUserId;
    }

    public void setBotUserId(String botUserId) {
        this.botUserId = botUserId;
    }

    public String getBotAccessToken() {
        return botAccessToken;
    }

    public void setBotAccessToken(String botAccessToken) {
        this.botAccessToken = botAccessToken;
        this.updatedAt = Instant.now();
    }

    public String getUserAccessToken() {
        return userAccessToken;
    }

    public void setUserAccessToken(String userAccessToken) {
        this.userAccessToken = userAccessToken;
        this.updatedAt = Instant.now();
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SlackToken{" +
                "id='" + id + '\'' +
                ", teamId='" + teamId + '\'' +
                ", teamName='" + teamName + '\'' +
                ", botUserId='" + botUserId + '\'' +
                ", scope='" + scope + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

