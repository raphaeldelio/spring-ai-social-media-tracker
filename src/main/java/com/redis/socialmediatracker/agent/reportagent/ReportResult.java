package com.redis.socialmediatracker.agent.reportagent;

import com.redis.om.spring.annotations.Indexed;

import java.util.List;

public class ReportResult {

    public enum FinishReason {
        COMPLETED,
        ERROR
    }

    @Indexed
    private FinishReason finishReason;

    @Indexed
    private String timeframe;

    @Indexed
    private Report report;

    @Indexed
    private Long tokens;

    public ReportResult() {}

    public ReportResult(FinishReason finishReason, String timeframe, Report report) {
        this.finishReason = finishReason;
        this.timeframe = timeframe;
        this.report = report;
    }

    // Static factory methods
    public static ReportResult completed(String timeframe, Report report) {
        return new ReportResult(FinishReason.COMPLETED, timeframe, report);
    }

    public static ReportResult error() {
        return new ReportResult(FinishReason.ERROR, null, null);
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

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public Long getTokens() {
        return tokens;
    }

    public void setTokens(Long tokens) {
        this.tokens = tokens;
    }

    @Override
    public String toString() {
        return "ReportResult{" +
                "finishReason=" + finishReason +
                ", timeframe='" + timeframe + '\'' +
                ", report=" + report +
                ", tokens=" + tokens +
                '}';
    }

    // Nested classes
    public static class Report {
        private String title;
        private String summary;
        private List<Section> sections;

        public Report() {}

        public Report(String title, String summary, List<Section> sections) {
            this.title = title;
            this.summary = summary;
            this.sections = sections;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<Section> getSections() {
            return sections;
        }

        public void setSections(List<Section> sections) {
            this.sections = sections;
        }

        @Override
        public String toString() {
            return "Report{" +
                    "title='" + title + '\'' +
                    ", summary='" + summary + '\'' +
                    ", sections=" + sections +
                    '}';
        }
    }

    public static class Section {
        private String heading;
        private String content;
        private List<Reference> references; // ✅ new field

        public Section() {}

        public Section(String heading, String content, List<Reference> references) {
            this.heading = heading;
            this.content = content;
            this.references = references;
        }

        public String getHeading() {
            return heading;
        }

        public void setHeading(String heading) {
            this.heading = heading;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<Reference> getReferences() {  // ✅ getter
            return references;
        }

        public void setReferences(List<Reference> references) {  // ✅ setter
            this.references = references;
        }

        @Override
        public String toString() {
            return "Section{" +
                    "heading='" + heading + '\'' +
                    ", content='" + content + '\'' +
                    ", references=" + references +
                    '}';
        }
    }

    public static class Reference {
        private String platform;
        private String url;
        private String description; // optional (e.g., “Post comparing Redis vs Dragonfly”)

        public Reference() {}

        public Reference(String platform, String url, String description) {
            this.platform = platform;
            this.url = url;
            this.description = description;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return "Reference{" +
                    "platform='" + platform + '\'' +
                    ", url='" + url + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}