package com.dataexchange.client.config;

import com.dataexchange.client.config.model.MainConfiguration;
import com.dataexchange.client.infrastructure.ErrorHandler;
import org.aopalliance.aop.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;

@Configuration
@IntegrationComponentScan
@EnableIntegration
@EnableConfigurationProperties(MainConfiguration.class)
public class FetchFileConfig {

    @Autowired
    private Advice pollerUpdateAdvice;

    @Bean
    public Advice moveFileAdvice() {
        ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
        advice.setOnSuccessExpressionString("payload");
        advice.setSuccessChannel(moveFileChannel());

        return advice;
    }

    @Bean
    IntegrationFlow moveFileFlow() {
        Expression directoryExpression = new SpelExpressionParser().parseExpression("inputMessage.headers['destination_folder']");

        return IntegrationFlows
                .from("moveFileChannel")
                .handle(Files.outboundAdapter(directoryExpression)
                        .fileExistsMode(FileExistsMode.REPLACE)
                        .deleteSourceFiles(true), conf -> conf.advice(pollerUpdateAdvice)
                )
                .get();
    }

    @Bean
    IntegrationFlow errorChannelFlow() {
        return IntegrationFlows
                .from("errorChannel")
                .handle(errorHandler())
                .get();
    }

    @Bean
    MessageChannel moveFileChannel() {
        return MessageChannels.queue().get();
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller() {
        return Pollers.fixedRate(2000).maxMessagesPerPoll(1).get();
    }

    @Bean
    public ErrorHandler errorHandler() {
        return new ErrorHandler();
    }
}
