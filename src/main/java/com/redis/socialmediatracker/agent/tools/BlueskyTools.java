package com.redis.socialmediatracker.agent.tools;

import com.redis.socialmediatracker.bluesky.BlueskyService;
import com.redis.socialmediatracker.bluesky.model.Post;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class BlueskyTools {

    private BlueskyService blueskyService;

    public BlueskyTools(BlueskyService blueskyService) {
        this.blueskyService = blueskyService;
    }

    @Tool
    public List<Post> searchPosts(
            String tag,

            @ToolParam(description = "A date-time with an offset from UTC/Greenwich in the ISO-8601 calendar system, such as 2007-12-03T10:15:30+01:00.")
            OffsetDateTime since
    ) {
        return blueskyService.searchPosts(
                tag,
                since,
                100
        );
    }
}