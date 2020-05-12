package com.dataexchange.client.config;

import com.dataexchange.client.config.model.RemoteConnectionConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
@RefreshScope
@ConfigurationProperties
public class RemoteConfiguration {

    private Map<String, RemoteConnectionConfiguration> connections;

    public RemoteConnectionConfiguration getConnectionConfigByName(String configName) {
        return connections.get(configName);
    }

    public Map<String, RemoteConnectionConfiguration> getConnections() {
        return connections;
    }

    public void setConnections(Map<String, RemoteConnectionConfiguration> connections) {
        this.connections = connections;
    }

}
