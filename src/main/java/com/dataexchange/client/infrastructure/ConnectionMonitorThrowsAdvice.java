package com.dataexchange.client.infrastructure;

import com.dataexchange.client.domain.ConnectionMonitor;
import org.springframework.aop.ThrowsAdvice;

public class ConnectionMonitorThrowsAdvice implements ThrowsAdvice {

    private final String connectionName;
    private final ConnectionMonitor connectionMonitor;

    public ConnectionMonitorThrowsAdvice(String connectionName, ConnectionMonitor connectionMonitor) {
        this.connectionName = connectionName;
        this.connectionMonitor = connectionMonitor;
    }

    public void afterThrowing(Exception ex) {
        connectionMonitor.down(connectionName, ex);
    }
}
