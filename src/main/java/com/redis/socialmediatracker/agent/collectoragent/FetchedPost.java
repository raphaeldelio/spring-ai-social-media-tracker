package com.redis.socialmediatracker.agent.collectoragent;

import java.util.Map;
import java.util.Objects;

public class FetchedPost {
    private String platform;
    private String postId;
    private String url;
    private String content;
    private int engagement;
    private String sentiment;
    private Map<String, Object> metadata;

    public FetchedPost(String platform, String postId, String url, String content,
                       int engagement, String sentiment, Map<String, Object> metadata) {
        this.platform = platform;
        this.postId = postId;
        this.url = url;
        this.content = content;
        this.engagement = engagement;
        this.sentiment = sentiment;
        this.metadata = metadata;
    }

    public String getPlatform() { return platform; }
    public String getPostId() { return postId; }
    public String getUrl() { return url; }
    public String getContent() { return content; }
    public int getEngagement() { return engagement; }
    public String getSentiment() { return sentiment; }
    public Map<String, Object> getMetadata() { return metadata; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FetchedPost)) return false;
        FetchedPost that = (FetchedPost) o;
        return engagement == that.engagement &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(postId, that.postId) &&
                Objects.equals(url, that.url) &&
                Objects.equals(content, that.content) &&
                Objects.equals(sentiment, that.sentiment) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, postId, url, content, engagement, sentiment, metadata);
    }

    @Override
    public String toString() {
        return "FetchedPost{" +
                "platform='" + platform + '\'' +
                ", postId='" + postId + '\'' +
                ", url='" + url + '\'' +
                ", content='" + content + '\'' +
                ", engagement=" + engagement +
                ", sentiment='" + sentiment + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}