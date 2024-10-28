package com.wyona.katie.health;

import com.wyona.katie.handlers.SentenceBERTQuestionAnswerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * Also see configuration parameter 'management.health.sentence-bert.enabled' inside application.properties
 */
@Slf4j
@Component
@ConditionalOnEnabledHealthIndicator("sentence-bert")
public class SentenceBERTHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private SentenceBERTQuestionAnswerImpl sbertImpl;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // Use the builder to build the health status details that should be reported.
        // If you throw an exception, the status will be DOWN with the exception message.

        String endpoint = "/api/v1/health";
        String host = sbertImpl.getHttpHost().toString();
        if (sbertImpl.isAlive(endpoint)) {
            builder.up().withDetail("endpoint", endpoint).withDetail("host", host);
        } else {
            builder.down().withDetail("endpoint", endpoint).withDetail("host", host);
        }
    }
}
