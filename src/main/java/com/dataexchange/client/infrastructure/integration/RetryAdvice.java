package com.dataexchange.client.infrastructure.integration;

import org.aopalliance.aop.Advice;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public final class RetryAdvice {

    public static Advice retry() {
        RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(10_000);
        exponentialBackOffPolicy.setMaxInterval(60_000);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
        retryTemplate.setRetryPolicy(new AlwaysRetryPolicy());
        advice.setRetryTemplate(retryTemplate);

        return advice;
    }
}
