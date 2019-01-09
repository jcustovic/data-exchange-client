package com.dataexchange.client.web;

import com.dataexchange.client.domain.model.exception.NoConnection;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ControllerAdvice
public class ControllerErrorHandler {

    @ExceptionHandler(NoConnection.class)
    @ResponseStatus(NOT_FOUND)
    public void handleNoConnection() {
    }
}
