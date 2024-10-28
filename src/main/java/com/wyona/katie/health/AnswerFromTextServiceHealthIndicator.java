package com.wyona.katie.health;

import com.wyona.katie.services.AnswerFromTextServiceRestImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * Also see configuration parameter 'management.health.answer-from-text-service.enabled' inside application.properties
 */
@Slf4j
@Component
@ConditionalOnEnabledHealthIndicator("answer-from-text-service")
public class AnswerFromTextServiceHealthIndicator extends AbstractHealthIndicator {

    @Value("${aft.rest.impl.host}")
    private String host;

    @Autowired
    private AnswerFromTextServiceRestImpl answerFromTextServiceRest;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // Use the builder to build the health status details that should be reported.
        // If you throw an exception, the status will be DOWN with the exception message.

        String endpoint = "/api/v1/health";
        if (answerFromTextServiceRest.isAlive(endpoint)) {
            builder.up().withDetail("endpoint", endpoint).withDetail("host", host);
        } else {
            builder.down().withDetail("endpoint", endpoint).withDetail("host", host);
        }
    }
}
