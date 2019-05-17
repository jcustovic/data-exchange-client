package com.dataexchange.client.domain.util;

import com.dataexchange.client.domain.ConnectionMonitor;
import com.dataexchange.client.infrastructure.ConnectionMonitorThrowsAdvice;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConnectionMonitorHelper {

    @Autowired
    private ConnectionMonitor connectionMonitor;

    public void handleConnectionError(String name, Throwable e) {
        connectionMonitor.down(name, e);
    }

    public AfterReturningAdvice connectionSuccessAdvice(final String connectionName) {
        return (returnValue, method, args, target) -> handleConnectionUp(connectionName);
    }

    public ThrowsAdvice connectionErrorAdvice(String connectionName) {
        return new ConnectionMonitorThrowsAdvice(connectionName, connectionMonitor);
    }

    private void handleConnectionUp(String name) {
        connectionMonitor.up(name);
    }
}
