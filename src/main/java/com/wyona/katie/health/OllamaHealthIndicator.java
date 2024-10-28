package com.wyona.katie.health;

import com.wyona.katie.handlers.OllamaGenerate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * Also see configuration parameter 'management.health.ollama.enabled' inside application.properties
 */
@Slf4j
@Component
@ConditionalOnEnabledHealthIndicator("ollama")
public class OllamaHealthIndicator extends AbstractHealthIndicator {

    @Value("${ollama.host}")
    private String ollamaHost;

    @Autowired
    private OllamaGenerate ollamaGenerate;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // Use the builder to build the health status details that should be reported.
        // If you throw an exception, the status will be DOWN with the exception message.

        String host = ollamaHost;
        if (ollamaGenerate.isAlive()) {
            builder.up().withDetail("host", host);
        } else {
            builder.down().withDetail("host", host);
        }
    }
}
