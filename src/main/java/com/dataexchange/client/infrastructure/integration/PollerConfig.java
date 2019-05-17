package com.dataexchange.client.infrastructure.integration;

import com.dataexchange.client.config.model.DownloadPollerConfiguration;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;

public final class PollerConfig {

    public static Consumer<SourcePollingChannelAdapterSpec> secondsPoller(int sec) {
        return conf -> conf.poller(Pollers.fixedRate(sec * 1000).maxMessagesPerPoll(-1));
    }

    public static Consumer<SourcePollingChannelAdapterSpec> minutesPoller(int min) {
        return conf -> conf.poller(Pollers.fixedRate(min * 60_000).maxMessagesPerPoll(-1));
    }

    public static PollerSpec configureDownloadPoller(DownloadPollerConfiguration pollerConfig) {
        if (StringUtils.hasText(pollerConfig.getPollCron())) {
            return Pollers.cron(pollerConfig.getPollCron());
        } else {
            return Pollers.fixedRate(pollerConfig.getPollIntervalMilliseconds());
        }
    }
}
