package com.myseotoolbox.testUtils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class LogEventMatcherBuilder {
    private Level level;
    private String messageContaining;


    public static LogEventMatcherBuilder logEvent() {
        return new LogEventMatcherBuilder();
    }

    public LogEventMatcherBuilder withLevel(Level level) {
        this.level = level;
        return this;
    }

    public LogEventMatcherBuilder withMessageContaining(String messageContaining) {
        this.messageContaining = messageContaining;
        return this;
    }

    public Matcher<LogEvent> build() {
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object item) {
                LogEvent compare = (LogEvent) item;
                return compare.getMessage().toString().contains(messageContaining) && compare.getLevel().equals(level);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(describeExpected());
            }
        };

    }

    public String describeExpected() {
        return "LogEvent{" +
                "level=" + level +
                ", messageContaining='" + messageContaining + '\'' +
                '}';
    }
}
