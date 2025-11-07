package com.redis.socialmediatracker.bluesky.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a Bluesky post.
 * Immutable once created, with explicit parsing from raw map data.
 */
public class Post {

    private final String uri;
    private final String cid;
    private final String author;
    private final String text;
    private final OffsetDateTime createdAt;
    private final int replyCount;
    private final int repostCount;
    private final int likeCount;

    public Post(String uri, String cid, String author, String text,
                OffsetDateTime createdAt, int replyCount, int repostCount, int likeCount) {
        this.uri = uri;
        this.cid = cid;
        this.author = author;
        this.text = text;
        this.createdAt = createdAt;
        this.replyCount = replyCount;
        this.repostCount = repostCount;
        this.likeCount = likeCount;
    }

    @SuppressWarnings("unchecked")
    public static Post fromMap(Map<String, Object> data) {
        Objects.requireNonNull(data, "Post data cannot be null");

        Map<String, Object> authorMap = (Map<String, Object>) data.get("author");
        Map<String, Object> recordMap = (Map<String, Object>) data.get("record");

        String uri = (String) data.get("uri");
        String cid = (String) data.get("cid");
        String author = authorMap != null ? (String) authorMap.get("handle") : null;
        String text = recordMap != null ? (String) recordMap.get("text") : null;
        String createdAtStr = recordMap != null ? (String) recordMap.get("createdAt") : null;

        OffsetDateTime createdAt = createdAtStr != null ? OffsetDateTime.parse(createdAtStr) : null;

        int replyCount = ((Number) data.getOrDefault("replyCount", 0)).intValue();
        int repostCount = ((Number) data.getOrDefault("repostCount", 0)).intValue();
        int likeCount = ((Number) data.getOrDefault("likeCount", 0)).intValue();

        return new Post(uri, cid, author, text, createdAt, replyCount, repostCount, likeCount);
    }

    public String getUri() {
        return uri;
    }

    public String getCid() {
        return cid;
    }

    public String getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public int getRepostCount() {
        return repostCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    /**
     * Builds a human-readable Bluesky web URL for this post.
     * Example: https://bsky.app/profile/johndoe.bsky.social/post/3k4tuwz45j72x
     * If author handle is missing, falls back to DID from the URI.
     */
    public String getUrl() {
        if (uri == null) {
            return null;
        }

        // Example URI: at://did:plc:abcd1234/app.bsky.feed.post/3k4tuwz45j72x
        try {
            String[] parts = uri.split("/");
            if (parts.length < 5) return null;

            String did = parts[2]; // did:plc:abcd1234
            String postId = parts[parts.length - 1];

            // Prefer the human-readable handle if available
            String profileId = (author != null && !author.isBlank()) ? author : did;

            // Encode the profile ID to handle colons in DIDs
            String encodedProfile = URLEncoder.encode(profileId, StandardCharsets.UTF_8);

            return "https://bsky.app/profile/" + encodedProfile + "/post/" + postId;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "Post{" +
                "uri='" + uri + '\'' +
                ", cid='" + cid + '\'' +
                ", author='" + author + '\'' +
                ", text='" + text + '\'' +
                ", createdAt=" + createdAt +
                ", replyCount=" + replyCount +
                ", repostCount=" + repostCount +
                ", likeCount=" + likeCount +
                ", url='" + getUrl() + '\'' +
                '}';
    }
}