package com.dataexchange.client.domain;

import com.dataexchange.client.domain.model.ConnectionStatus;
import com.dataexchange.client.domain.model.PollerStatus;

import java.util.Map;

public interface ConnectionMonitor {

    void register(String connectionName, Map<String, PollerStatus> pollerStatus);

    void up(String connectionName);

    void down(String connectionName, Throwable e);

    Map<String, ConnectionStatus> getConnectionStatuses();

    ConnectionStatus findConnectionStatus(String connectionName);

    void updatePollerStatus(String connectionName, String pollerName, String filename);

    Object evaluateExpression(String connectionName, String expression);

    Object evaluateExpression(String connectionName, String pollerName, String expression);
}
