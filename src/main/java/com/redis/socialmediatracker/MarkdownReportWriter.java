package com.redis.socialmediatracker;

import com.redis.socialmediatracker.agent.reportagent.ReportResult;
import java.util.List;

/**
 * Converts a ReportResult object into a Markdown-formatted string.
 */
public class MarkdownReportWriter {

    /**
     * Converts a ReportResult object into a Markdown-formatted string.
     *
     * @param reportResult The structured report output from the ReportAgent.
     * @return A Markdown string representation of the report.
     */
    public static String toMarkdown(ReportResult reportResult) {
        if (reportResult == null || reportResult.getReport() == null) {
            return "# Redis Social Trends Report\n\n_No report data available._";
        }

        var report = reportResult.getReport();
        StringBuilder md = new StringBuilder();

        // Title
        md.append("# ")
                .append(report.getTitle() != null ? report.getTitle() : "Redis Social Trends Report")
                .append("\n\n");

        // Timeframe
        if (reportResult.getTimeframe() != null) {
            md.append("> **Timeframe:** ").append(reportResult.getTimeframe()).append("\n\n");
        }

        // Summary
        if (report.getSummary() != null && !report.getSummary().isBlank()) {
            md.append("## Summary\n")
                    .append(report.getSummary())
                    .append("\n\n");
        }

        // Sections
        if (report.getSections() != null && !report.getSections().isEmpty()) {
            for (ReportResult.Section section : report.getSections()) {
                md.append("## ").append(section.getHeading()).append("\n")
                        .append(section.getContent()).append("\n\n");

                // ✅ Add References if available
                List<ReportResult.Reference> refs = section.getReferences();
                if (refs != null && !refs.isEmpty()) {
                    md.append("### Sources & References\n");
                    for (int i = 0; i < refs.size(); i++) {
                        ReportResult.Reference ref = refs.get(i);

                        // Construct markdown link
                        String linkText = ref.getPlatform() != null
                                ? "[" + ref.getPlatform() + "](" + ref.getUrl() + ")"
                                : ref.getUrl();

                        md.append("- ").append(linkText);

                        if (ref.getDescription() != null && !ref.getDescription().isBlank()) {
                            md.append(" — ").append(ref.getDescription());
                        }

                        md.append("\n");
                    }
                    md.append("\n");
                }
            }
        } else {
            md.append("_No sections available in this report._\n\n");
        }

        // Footer
        md.append("---\n")
                .append("_Generated automatically by the Redis Social Media Tracker_\n");

        return md.toString();
    }
}