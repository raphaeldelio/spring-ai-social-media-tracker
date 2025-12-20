package com.redis.socialmediatracker.agent.analysisagent;

import com.redis.om.spring.annotations.Indexed;

import java.util.List;
import java.util.Map;

public class AnalysisResult {

    public enum FinishReason {
        COMPLETED,
        ERROR
    }

    @Indexed
    private FinishReason finishReason;

    @Indexed
    private String timeframe;

    @Indexed
    private List<TopicResult> topics;

    public AnalysisResult() {}

    public AnalysisResult(FinishReason finishReason, String timeframe, List<TopicResult> topics) {
        this.finishReason = finishReason;
        this.timeframe = timeframe;
        this.topics = topics;
    }

    // Static factory methods
    public static AnalysisResult completed(String timeframe, List<TopicResult> topics) {
        return new AnalysisResult(FinishReason.COMPLETED, timeframe, topics);
    }

    public static AnalysisResult error() {
        return new AnalysisResult(FinishReason.ERROR, null, null);
    }

    // Getters and setters
    public FinishReason getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(FinishReason finishReason) {
        this.finishReason = finishReason;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public List<TopicResult> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicResult> topics) {
        this.topics = topics;
    }

    @Override
    public String toString() {
        return "TrendAnalysisResult{" +
                "finishReason=" + finishReason +
                ", timeframe='" + timeframe + '\'' +
                ", topics=" + topics +
                '}';
    }

    // Nested classes
    public static class TopicResult {
        private String topic;
        private boolean trending;
        private Metrics metrics;
        private List<Source> sources;

        public TopicResult() {}

        public TopicResult(String topic, boolean trending, Metrics metrics, List<Source> sources) {
            this.topic = topic;
            this.trending = trending;
            this.metrics = metrics;
            this.sources = sources;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public boolean isTrending() {
            return trending;
        }

        public void setTrending(boolean trending) {
            this.trending = trending;
        }

        public Metrics getMetrics() {
            return metrics;
        }

        public void setMetrics(Metrics metrics) {
            this.metrics = metrics;
        }

        public List<Source> getSources() {
            return sources;
        }

        public void setSources(List<Source> sources) {
            this.sources = sources;
        }

        @Override
        public String toString() {
            return "TopicResult{" +
                    "topic='" + topic + '\'' +
                    ", trending=" + trending +
                    ", metrics=" + metrics +
                    ", sources=" + sources +
                    '}';
        }
    }

    public static class Metrics {
        private int postCount;
        private int totalEngagement;
        private double averageEngagement;
        private Map<String, Integer> sentimentBreakdown;
        private List<String> topKeywords;

        public Metrics() {}

        public Metrics(int postCount, int totalEngagement, double averageEngagement,
                       Map<String, Integer> sentimentBreakdown, List<String> topKeywords) {
            this.postCount = postCount;
            this.totalEngagement = totalEngagement;
            this.averageEngagement = averageEngagement;
            this.sentimentBreakdown = sentimentBreakdown;
            this.topKeywords = topKeywords;
        }

        public int getPostCount() {
            return postCount;
        }

        public void setPostCount(int postCount) {
            this.postCount = postCount;
        }

        public int getTotalEngagement() {
            return totalEngagement;
        }

        public void setTotalEngagement(int totalEngagement) {
            this.totalEngagement = totalEngagement;
        }

        public double getAverageEngagement() {
            return averageEngagement;
        }

        public void setAverageEngagement(double averageEngagement) {
            this.averageEngagement = averageEngagement;
        }

        public Map<String, Integer> getSentimentBreakdown() {
            return sentimentBreakdown;
        }

        public void setSentimentBreakdown(Map<String, Integer> sentimentBreakdown) {
            this.sentimentBreakdown = sentimentBreakdown;
        }

        public List<String> getTopKeywords() {
            return topKeywords;
        }

        public void setTopKeywords(List<String> topKeywords) {
            this.topKeywords = topKeywords;
        }

        @Override
        public String toString() {
            return "Metrics{" +
                    "postCount=" + postCount +
                    ", totalEngagement=" + totalEngagement +
                    ", averageEngagement=" + averageEngagement +
                    ", sentimentBreakdown=" + sentimentBreakdown +
                    ", topKeywords=" + topKeywords +
                    '}';
        }
    }

    public static class Source {
        private String platform;
        private String postId;
        private String url;

        public Source() {}

        public Source(String platform, String postId, String url) {
            this.platform = platform;
            this.postId = postId;
            this.url = url;
        }

        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }

        public String getPostId() { return postId; }
        public void setPostId(String postId) { this.postId = postId; }

        public String getUrl() { return url; }      // ✅ new getter
        public void setUrl(String url) { this.url = url; }  // ✅ new setter

        @Override
        public String toString() {
            return "Source{" +
                    "platform='" + platform + '\'' +
                    ", postId='" + postId + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}