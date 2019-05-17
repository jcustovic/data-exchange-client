package com.dataexchange.client.config;

import com.dataexchange.client.domain.ConnectionMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.messaging.Message;

@Configuration
public class MonitoringConfig {

    @Autowired
    private ConnectionMonitor connectionMonitor;

    @Bean
    public AbstractRequestHandlerAdvice pollerUpdateAdvice() {
        return new AbstractRequestHandlerAdvice() {
            @Override
            protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
                Message unwrap = message;
                if (message instanceof AdviceMessage) {
                    unwrap = ((AdviceMessage<?>) message).getInputMessage();
                }
                String pollerName = unwrap.getHeaders().get("poller_name", String.class);
                String connectionName = unwrap.getHeaders().get("connection_name", String.class);
                String filename = unwrap.getHeaders().get("file_name", String.class);

                connectionMonitor.updatePollerStatus(connectionName, pollerName, filename);

                return callback.execute();
            }
        };
    }
}
