package com.redis.socialmediatracker;

import com.redis.socialmediatracker.agent.analysisagent.AnalysisAgent;
import com.redis.socialmediatracker.agent.collectoragent.CrawlerAgent;
import com.redis.socialmediatracker.agent.insightsagent.InsightAgent;
import com.redis.socialmediatracker.agent.reportagent.ReportAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * CLI Runner for testing the agent pipeline.
 * This is disabled by default when running in Slack mode.
 * To enable it, set: app.cli.enabled=true
 */
@Component
@ConditionalOnProperty(name = "app.cli.enabled", havingValue = "true", matchIfMissing = false)
public class Runner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    private final CrawlerAgent crawlerAgent;
    private final AnalysisAgent analysisAgent;
    private final InsightAgent insightAgent;
    private final ReportAgent reportAgent;

    public Runner(
            CrawlerAgent crawlerAgent,
            AnalysisAgent analysisAgent,
            InsightAgent insightAgent,
            ReportAgent reportAgent) {
        this.crawlerAgent = crawlerAgent;
        this.analysisAgent = analysisAgent;
        this.insightAgent = insightAgent;
        this.reportAgent = reportAgent;
    }

    @Override
    public void run(String... args) {
        LOGGER.info("🚀 Running CLI mode...");

        String userMessage = """
                Search for posts related to Redis, the data platform.
                """;

        var crawlerResult = crawlerAgent.run(userMessage);
        var analysisResult = analysisAgent.run(crawlerResult.entity());
        var insightResult = insightAgent.run(crawlerResult.entity(), analysisResult.entity());
        var reportResult = reportAgent.run(crawlerResult.entity(), analysisResult.entity(), insightResult.entity());

        LOGGER.info("Insights result: {}", MarkdownReportWriter.toMarkdown(reportResult.entity()));
    }
}