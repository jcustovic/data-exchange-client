package com.dataexchange.client.infrastructure;

import com.dataexchange.client.domain.ConnectionMonitor;
import com.dataexchange.client.domain.model.ConnectionStatus;
import com.dataexchange.client.domain.model.PollerStatus;
import com.dataexchange.client.domain.model.exception.ConnectionAlreadyRegistered;
import com.dataexchange.client.domain.model.exception.NoConnection;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class ConnectionMonitorImpl implements ConnectionMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionMonitorImpl.class);

    private final Map<String, ConnectionStatus> connections;
    private final ExpressionParser parser;

    public ConnectionMonitorImpl() {
        connections = Collections.synchronizedMap(new HashMap<>());
        parser = new SpelExpressionParser();
    }

    @Override
    public void register(String connectionName, Map<String, PollerStatus> pollerStatus) {
        if (connections.containsKey(connectionName)) {
            throw new ConnectionAlreadyRegistered("Already existing connection monitoring entry with key " + connectionName);
        }
        connections.put(connectionName, new ConnectionStatus(pollerStatus));
    }

    @Override
    public void up(String connectionName) {
        findConnectionStatus(connectionName).up();
    }

    @Override
    public void down(String connectionName, Throwable e) {
        String message = "Connection down for " + connectionName + ". Cause: " + ExceptionUtils.getRootCauseMessage(e);
        findConnectionStatus(connectionName).down(message);
        LOGGER.error("Connection down", e);
    }

    @Override
    public Map<String, ConnectionStatus> getConnectionStatuses() {
        return Collections.unmodifiableMap(connections);
    }

    @Override
    public ConnectionStatus findConnectionStatus(String connectionName) {
        ConnectionStatus connectionStatus = connections.get(connectionName);
        if (connectionStatus == null) {
            throw new NoConnection("No connection found with name " + connectionName);
        }

        return connectionStatus;
    }

    @Override
    public void updatePollerStatus(String connectionName, String pollerName, String filename) {
        findConnectionStatus(connectionName).updatePoller(pollerName, filename, LocalDateTime.now());
    }

    @Override
    public Object evaluateExpression(String connectionName, String expression) {
        Expression exp = parser.parseExpression(expression);

        EvaluationContext context = new StandardEvaluationContext(findConnectionStatus(connectionName));

        return exp.getValue(context);
    }

    @Override
    public Object evaluateExpression(String connectionName, String pollerName, String expression) {
        Expression exp = parser.parseExpression(expression);

        EvaluationContext context = new StandardEvaluationContext(findConnectionStatus(connectionName).getPollers().get(pollerName));

        return exp.getValue(context);
    }
}
