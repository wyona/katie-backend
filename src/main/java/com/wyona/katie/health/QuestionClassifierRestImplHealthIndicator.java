package com.wyona.katie.health;

import com.wyona.katie.handlers.qc.QuestionClassifierRestImpl;
import com.wyona.katie.models.QuestionClassificationImpl;
import com.wyona.katie.handlers.qc.QuestionClassifierRestImpl;
import com.wyona.katie.models.QuestionClassificationImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * Also see configuration parameter 'management.endpoint.health.show-details' inside application.properties
 */
@Slf4j
@Component
public class QuestionClassifierRestImplHealthIndicator extends AbstractHealthIndicator {

    @Value("${qc.implementation}")
    private QuestionClassificationImpl qcImpl;

    @Autowired
    private QuestionClassifierRestImpl questionClassifyImpl;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // Use the builder to build the health status details that should be reported.
        // If you throw an exception, the status will be DOWN with the exception message.

        String endpoint = "/api/v1/health";
        log.info("Configured question classification implementation: " + qcImpl);
        if (qcImpl.equals(QuestionClassificationImpl.REST)) {
            if (questionClassifyImpl.isAlive(endpoint)) {
                builder.up()
                        .withDetail("endpoint", endpoint);
            } else {
                builder.down()
                        .withDetail("endpoint", endpoint);
            }
        } else {
            log.info("QuestionClassifierRestImpl is not enabled, therefore do not check whether it is alive.");
            builder.unknown();
        }
    }
}
