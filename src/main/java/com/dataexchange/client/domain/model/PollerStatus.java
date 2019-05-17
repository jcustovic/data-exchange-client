package com.dataexchange.client.domain.model;

import java.time.LocalDateTime;

public class PollerStatus {

    public enum PollerStatusDirection {
        UPLOAD, DOWNLOAD
    }

    private PollerStatusDirection direction;
    private LocalDateTime lastTransfer;
    private String lastFilename;

    public PollerStatus(PollerStatusDirection direction) {
        this.direction = direction;
    }

    public void update(String filename, LocalDateTime transferTime) {
        this.lastFilename = filename;
        this.lastTransfer = transferTime;
    }

    public PollerStatusDirection getDirection() {
        return direction;
    }

    public LocalDateTime getLastTransfer() {
        return lastTransfer;
    }

    public String getLastFilename() {
        return lastFilename;
    }
}
