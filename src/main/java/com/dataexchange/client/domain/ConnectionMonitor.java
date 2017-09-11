package com.dataexchange.client.domain;

import com.dataexchange.client.domain.model.ConnectionStatus;

import java.util.Map;

public interface ConnectionMonitor {

    void register(String connectionName);

    void up(String connectionName);

    void down(String connectionName, Throwable e);

    Map<String, ConnectionStatus> getConnectionStatuses();
}
