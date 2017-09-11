package com.dataexchange.client.web;

import com.dataexchange.client.domain.ConnectionMonitor;
import com.dataexchange.client.domain.model.ConnectionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
}
