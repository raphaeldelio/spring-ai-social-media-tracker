package com.redis.socialmediatracker.agent.insightsagent;

import com.redis.om.spring.annotations.Indexed;

import java.util.List;
import java.util.Map;

public class InsightResult {

    public enum FinishReason {
        COMPLETED,
        ERROR
    }

    @Indexed
    private FinishReason finishReason;

    @Indexed
    private String timeframe;

    @Indexed
    private Insights insights;

    public InsightResult() {}

    public InsightResult(FinishReason finishReason, String timeframe, Insights insights) {
        this.finishReason = finishReason;
        this.timeframe = timeframe;
        this.insights = insights;
    }

    // Static factory methods
    public static InsightResult completed(String timeframe, Insights insights) {
        return new InsightResult(FinishReason.COMPLETED, timeframe, insights);
    }

    public static InsightResult error() {
        return new InsightResult(FinishReason.ERROR, null, null);
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

    public Insights getInsights() {
        return insights;
    }

    public void setInsights(Insights insights) {
        this.insights = insights;
    }

    @Override
    public String toString() {
        return "InsightResult{" +
                "finishReason=" + finishReason +
                ", timeframe='" + timeframe + '\'' +
                ", insights=" + insights +
                '}';
    }

    // Nested classes
    public static class Insights {
        @Indexed
        private List<Insight> descriptive;

        @Indexed
        private List<Insight> diagnostic;

        @Indexed
        private List<Insight> predictive;

        @Indexed
        private List<Insight> prescriptive;

        public Insights() {}

        public Insights(List<Insight> descriptive, List<Insight> diagnostic,
                        List<Insight> predictive, List<Insight> prescriptive) {
            this.descriptive = descriptive;
            this.diagnostic = diagnostic;
            this.predictive = predictive;
            this.prescriptive = prescriptive;
        }

        public List<Insight> getDescriptive() {
            return descriptive;
        }

        public void setDescriptive(List<Insight> descriptive) {
            this.descriptive = descriptive;
        }

        public List<Insight> getDiagnostic() {
            return diagnostic;
        }

        public void setDiagnostic(List<Insight> diagnostic) {
            this.diagnostic = diagnostic;
        }

        public List<Insight> getPredictive() {
            return predictive;
        }

        public void setPredictive(List<Insight> predictive) {
            this.predictive = predictive;
        }

        public List<Insight> getPrescriptive() {
            return prescriptive;
        }

        public void setPrescriptive(List<Insight> prescriptive) {
            this.prescriptive = prescriptive;
        }

        @Override
        public String toString() {
            return "Insights{" +
                    "descriptive=" + descriptive +
                    ", diagnostic=" + diagnostic +
                    ", predictive=" + predictive +
                    ", prescriptive=" + prescriptive +
                    '}';
        }
    }

    public static class Insight {
        @Indexed
        private String statement;

        @Indexed
        private Evidence evidence;

        public Insight() {}

        public Insight(String statement, Evidence evidence) {
            this.statement = statement;
            this.evidence = evidence;
        }

        public String getStatement() {
            return statement;
        }

        public void setStatement(String statement) {
            this.statement = statement;
        }

        public Evidence getEvidence() {
            return evidence;
        }

        public void setEvidence(Evidence evidence) {
            this.evidence = evidence;
        }

        @Override
        public String toString() {
            return "Insight{" +
                    "statement='" + statement + '\'' +
                    ", evidence=" + evidence +
                    '}';
        }
    }

    public static class Evidence {
        @Indexed
        private String topic;

        @Indexed
        private int engagement;

        @Indexed
        private Map<String, Integer> sentimentBreakdown;

        @Indexed
        private List<String> platforms;

        @Indexed
        private List<String> sourceUrls;

        @Indexed
        private Map<String, Object> additionalDetails;

        public Evidence() {}

        public Evidence(String topic, int engagement, Map<String, Integer> sentimentBreakdown,
                        List<String> platforms, List<String> sourceUrls,
                        Map<String, Object> additionalDetails) {
            this.topic = topic;
            this.engagement = engagement;
            this.sentimentBreakdown = sentimentBreakdown;
            this.platforms = platforms;
            this.sourceUrls = sourceUrls;
            this.additionalDetails = additionalDetails;
        }

        public List<String> getSourceUrls() { return sourceUrls; }
        public void setSourceUrls(List<String> sourceUrls) { this.sourceUrls = sourceUrls; }

        @Override
        public String toString() {
            return "Evidence{" +
                    "topic='" + topic + '\'' +
                    ", engagement=" + engagement +
                    ", sentimentBreakdown=" + sentimentBreakdown +
                    ", platforms=" + platforms +
                    ", sourceUrls=" + sourceUrls +
                    ", additionalDetails=" + additionalDetails +
                    '}';
        }
    }
}