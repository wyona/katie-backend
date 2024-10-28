package com.wyona.katie.health;

import com.wyona.katie.handlers.WeaviateQuestionAnswerImpl;
import com.wyona.katie.handlers.WeaviateQuestionAnswerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * Also see configuration parameter 'management.health.weaviate.enabled' inside application.properties
 */
@Slf4j
@Component
@ConditionalOnEnabledHealthIndicator("weaviate")
public class WeaviateHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private WeaviateQuestionAnswerImpl weaviateImpl;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // Use the builder to build the health status details that should be reported.
        // If you throw an exception, the status will be DOWN with the exception message.

        String host = weaviateImpl.getHost(null);
        String endpoint = "/v1";
        if (weaviateImpl.isAlive(endpoint)) {
            builder.up()
                    .withDetail("endpoint", endpoint).withDetail("version", weaviateImpl.getVersion(null)).withDetail("host", host);
        } else {
            builder.down()
                    .withDetail("endpoint", endpoint).withDetail("host", host);
        }
    }
}
