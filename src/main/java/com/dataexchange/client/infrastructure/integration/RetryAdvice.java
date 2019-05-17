package com.dataexchange.client.infrastructure.integration;

import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public final class RetryAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAdvice.class);

    public static Advice retry() {
        RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(10_000);
        exponentialBackOffPolicy.setMaxInterval(60_000);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(5));
        retryTemplate.registerListener(new RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
                                                         Throwable throwable) {
                LOGGER.warn("Unknown error... Will retry", throwable);
            }
        });
        advice.setRetryTemplate(retryTemplate);

        return advice;
    }
}
