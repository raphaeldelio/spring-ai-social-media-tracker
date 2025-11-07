package com.redis.socialmediatracker.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    public String getCurrentDateTime() {
        ZoneId userZone = LocaleContextHolder.getTimeZone().toZoneId();
        ZonedDateTime current = LocalDateTime.now().atZone(userZone);
        return current.toString();
    }
}