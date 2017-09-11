package com.dataexchange.client.infrastructure;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

public class ErrorHandler implements MessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        Throwable messageException = ((MessagingException) message.getPayload()).getCause();
        Throwable originalError = messageException.getCause();

        LOGGER.error(ExceptionUtils.getRootCauseMessage(originalError), originalError);
    }

}

