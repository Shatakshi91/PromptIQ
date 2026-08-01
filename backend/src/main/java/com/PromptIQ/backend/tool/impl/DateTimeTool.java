package com.PromptIQ.backend.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.PromptIQ.backend.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeTool implements Tool {

    @Override
    public String name() { return "get_current_datetime"; }

    @Override
    public String description() {
        return "Returns the current date and time in UTC. Use this whenever the user asks about "
                + "'today', 'now', 'current time', or needs a date-relative answer — never guess the date.";
    }

    @Override
    public String parametersJsonSchema() {
        return """
                { "type": "object", "properties": {} }
                """;
    }

    @Override
    public String execute(JsonNode arguments) {
        return ZonedDateTime.now(java.time.ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm 'UTC'"));
    }
}