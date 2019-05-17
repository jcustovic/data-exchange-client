package com.dataexchange.client.web;

import com.dataexchange.client.domain.ConnectionMonitor;
import com.dataexchange.client.domain.model.ConnectionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/monitoring/connection-health-check")
public class MonitoringController {

    @Autowired
    private ConnectionMonitor connectionMonitor;

    @GetMapping
    public Map<String, ConnectionStatus> findConnectionStatuses() {
        return connectionMonitor.getConnectionStatuses();
    }

    @GetMapping("/{connectionName}")
    public ConnectionStatus findOne(@PathVariable String connectionName) {
        return connectionMonitor.findConnectionStatus(connectionName);
    }

    @GetMapping(value = "/{connectionName}", params = "expression")
    public Object evaluateExpOnConnection(@PathVariable String connectionName, @RequestParam String expression) {
        return connectionMonitor.evaluateExpression(connectionName, expression);
    }

    @GetMapping(value = "/{connectionName}/{pollerName}")
    public Object evaluateExpOnPoller(@PathVariable String connectionName, @PathVariable String pollerName,
                                      @RequestParam String expression) {
        return connectionMonitor.evaluateExpression(connectionName, pollerName, expression);
    }
}
