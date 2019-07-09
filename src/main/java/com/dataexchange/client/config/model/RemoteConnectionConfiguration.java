package com.dataexchange.client.config.model;

import java.util.Map;

public class RemoteConnectionConfiguration {

    private String hostname;
    private String username;
    private String password;
    private String privateKey;
    private Integer port;
    private Map<FileType, RemoteFolders> remoteFolders;


    // Getters & setters

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Map<FileType, RemoteFolders> getRemoteFolders() {
        return remoteFolders;
    }

    public void setRemoteFolders(Map<FileType, RemoteFolders> remoteFolders) {
        this.remoteFolders = remoteFolders;
    }
}
