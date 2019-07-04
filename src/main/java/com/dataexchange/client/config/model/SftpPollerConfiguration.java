package com.dataexchange.client.config.model;

import org.springframework.core.io.Resource;

public class SftpPollerConfiguration extends BasePollerConfiguration {

    private Resource privateKey;
    private String privateKeyPassphrase;

    @Override
    public Integer getPort() {
        return super.getPort() == null ? 22 : super.getPort();
    }

    // Getters and Setters

    public Resource getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(Resource privateKey) {
        this.privateKey = privateKey;
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

}
