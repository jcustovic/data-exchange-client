package com.dataexchange.client.domain.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static com.dataexchange.client.domain.model.ConnectionStatus.ConnectionAliveStatus.*;

public class ConnectionStatus {

    enum ConnectionAliveStatus {
        UNKNOWN, UP, DOWN
    }

    private ConnectionAliveStatus status;
    private LocalDateTime lastCheck;
    private LocalDateTime downSince;
    private String lastError;
    private Map<String, PollerStatus> pollers;

    public ConnectionStatus(Map<String, PollerStatus> pollers) {
        this.pollers = pollers;
        status = UNKNOWN;
    }

    public void up() {
        status = UP;
        lastCheck = LocalDateTime.now();
        downSince = null;
        lastError = null;
    }

    public void down(String message) {
        if (status != DOWN) {
            downSince = LocalDateTime.now();
        }
        status = DOWN;
        lastCheck = LocalDateTime.now();
        lastError = message;
    }

    public void updatePoller(String name, String filename, LocalDateTime transferTime) {
        pollers.get(name).update(filename, transferTime);
    }

    public ConnectionAliveStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastCheck() {
        return lastCheck;
    }

    public LocalDateTime getDownSince() {
        return downSince;
    }

    public String getLastError() {
        return lastError;
    }

    public Map<String, PollerStatus> getPollers() {
        return Collections.unmodifiableMap(pollers);
    }
}
