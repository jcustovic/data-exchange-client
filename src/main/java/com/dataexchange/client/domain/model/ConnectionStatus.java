package com.dataexchange.client.domain.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dataexchange.client.domain.model.ConnectionStatus.ConnectionAliveStatus.*;

public class ConnectionStatus {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatus.class);

    enum ConnectionAliveStatus {
        UNKNOWN, UP, DOWN
    }

    public ConnectionStatus() {
        status = UNKNOWN;
    }

    private ConnectionAliveStatus status;
    private LocalDateTime lastCheck;
    private LocalDateTime downSince;
    private String lastError;
    private List<String> pollers = new ArrayList<>(); // TODO: Pollers status

    public void up() {
        status = UP;
        lastCheck = LocalDateTime.now();
        downSince = null;
        lastError = null;
    }

    public void down(String message) {
        if (status != DOWN) {
            LOGGER.error(message);
            downSince = LocalDateTime.now();

        }
        status = DOWN;
        lastCheck = LocalDateTime.now();
        lastError = message;
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

    public List<String> getPollers() {
        return Collections.unmodifiableList(pollers);
    }
}
