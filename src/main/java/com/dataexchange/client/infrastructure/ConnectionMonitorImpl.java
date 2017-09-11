package com.dataexchange.client.infrastructure;

import com.dataexchange.client.domain.ConnectionMonitor;
import com.dataexchange.client.domain.model.ConnectionStatus;
import com.dataexchange.client.domain.model.exception.ConnectionAlreadyRegistered;
import com.dataexchange.client.domain.model.exception.NoConnection;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class ConnectionMonitorImpl implements ConnectionMonitor {

    private final Map<String, ConnectionStatus> connections;

    public ConnectionMonitorImpl() {
        connections = Collections.synchronizedMap(new HashMap<String, ConnectionStatus>());
    }

    @Override
    public void register(String connectionName) {
        if (connections.containsKey(connectionName)) {
            throw new ConnectionAlreadyRegistered("Already existing connection monitoring entry with key " + connectionName);
        }
        connections.put(connectionName, new ConnectionStatus());
    }

    @Override
    public void up(String connectionName) {
        findConnectionStatus(connectionName).up();
    }

    @Override
    public void down(String connectionName, Throwable e) {
        String message = "Connection down for " + connectionName + ". Cause: " + ExceptionUtils.getRootCauseMessage(e);
        findConnectionStatus(connectionName).down(message);
    }

    @Override
    public Map<String, ConnectionStatus> getConnectionStatuses() {
        return Collections.unmodifiableMap(connections);
    }

    private ConnectionStatus findConnectionStatus(String connectionName) {
        ConnectionStatus connectionStatus = connections.get(connectionName);
        if (connectionStatus == null) {
            throw new NoConnection("No connection found with name " + connectionName);
        }

        return connectionStatus;
    }
}
