package com.dataexchange.client.web;

import com.dataexchange.client.domain.ConnectionMonitor;
import com.dataexchange.client.domain.model.ConnectionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
