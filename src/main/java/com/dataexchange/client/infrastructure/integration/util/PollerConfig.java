package com.dataexchange.client.infrastructure.integration.util;

import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;

import java.util.function.Consumer;

public final class PollerConfig {

    public static Consumer<SourcePollingChannelAdapterSpec> secondsPoller(int sec, int maxMessages) {
        return conf -> conf.poller(Pollers.fixedRate(sec * 1000).maxMessagesPerPoll(maxMessages));
    }

    public static Consumer<SourcePollingChannelAdapterSpec> minutesPoller(int min, int maxMessages) {
        return conf -> conf.poller(Pollers.fixedRate(min * 60_000).maxMessagesPerPoll(maxMessages));
    }
}
