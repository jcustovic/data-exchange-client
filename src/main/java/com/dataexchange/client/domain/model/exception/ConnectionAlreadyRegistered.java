package com.dataexchange.client.domain.model.exception;

public class ConnectionAlreadyRegistered extends RuntimeException {

    public ConnectionAlreadyRegistered(String message) {
        super(message);
    }
}
