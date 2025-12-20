package com.redis.socialmediatracker.agent;

import com.redis.socialmediatracker.agent.memory.ConversationState;
import com.redis.socialmediatracker.agent.memory.ConversationStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StartupRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(StartupRecoveryService.class);

    private final ConversationStateRepository conversationStateRepository;
    private final AgentOrchestrationService agentOrchestrationService;

    public StartupRecoveryService(
            ConversationStateRepository conversationStateRepository,
            AgentOrchestrationService agentOrchestrationService) {
        this.conversationStateRepository = conversationStateRepository;
        this.agentOrchestrationService = agentOrchestrationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedWorkflows() {
        logger.info("🔍 Checking for interrupted workflows to recover...");

        try {
            // Find all conversations that were running when the app shut down
            List<ConversationState> runningConversations = conversationStateRepository.findByIsRunning(true);

            if (runningConversations.isEmpty()) {
                logger.info("✅ No interrupted workflows found. All clear!");
                return;
            }

            logger.info("🔄 Found {} interrupted workflow(s) to recover", runningConversations.size());

            // Resume each conversation
            for (ConversationState state : runningConversations) {
                try {
                    logger.info("🔄 Recovering conversation {} (stage: {}, team: {}, channel: {})",
                            state.getConversationId(),
                            state.getCurrentStage(),
                            state.getTeamId(),
                            state.getChannel());

                    // Resume the conversation asynchronously
                    agentOrchestrationService.resumeConversation(state);

                    logger.info("✅ Queued conversation {} for recovery", state.getConversationId());

                } catch (Exception e) {
                    logger.error("❌ Failed to recover conversation {}: {}",
                            state.getConversationId(), e.getMessage(), e);

                    // Mark as not running to prevent retry loops
                    try {
                        state.setRunning(false);
                        conversationStateRepository.save(state);
                    } catch (Exception saveError) {
                        logger.error("❌ Failed to update conversation state: {}", saveError.getMessage());
                    }
                }
            }

            logger.info("🎉 Startup recovery completed. Queued {} workflow(s) for resumption",
                    runningConversations.size());

        } catch (Exception e) {
            logger.error("❌ Error during startup recovery: {}", e.getMessage(), e);
        }
    }
}

