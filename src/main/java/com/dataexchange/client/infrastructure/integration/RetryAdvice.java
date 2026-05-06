package com.dataexchange.client.infrastructure.integration;

import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.Retryable;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;

import java.time.Duration;

public final class RetryAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAdvice.class);

    public static Advice retry() {
        RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxRetries(5)
                .delay(Duration.ofSeconds(10))
                .multiplier(2.0)
                .maxDelay(Duration.ofSeconds(60))
                .build();

        advice.setRetryPolicy(retryPolicy);
        advice.setRetryListener(new RetryListener() {
            @Override
            public void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
                LOGGER.warn("Unknown error... Will retry", throwable);
            }
        });

        return advice;
    }
}
