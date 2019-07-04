package com.dataexchange.client.config.model;

import com.dataexchange.client.infrastructure.util.Conditional;
import org.springframework.util.StringUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Conditional(selected = "remoteConfigName", exists = false, required = {"host", "username"})
@Conditional(selected = "remoteConfigName", exists = true, required = {})
public class BasePollerConfiguration {

    @NotEmpty
    private String name;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String remoteConfigName;

    @Valid
    private List<UploadPollerConfiguration> uploadPollers = new ArrayList<>();
    @Valid
    private List<DownloadPollerConfiguration> downloadPollers = new ArrayList<>();

    public boolean hasRemoteConfig() {
        return !StringUtils.isEmpty(remoteConfigName);
    }

    // Getters & Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }


    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
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

    public String getRemoteConfigName() {
        return remoteConfigName;
    }

    public void setRemoteConfigName(String remoteConfigName) {
        this.remoteConfigName = remoteConfigName;
    }

    public List<UploadPollerConfiguration> getUploadPollers() {
        return uploadPollers;
    }

    public void setUploadPollers(List<UploadPollerConfiguration> uploadPollers) {
        this.uploadPollers = uploadPollers;
    }

    public List<DownloadPollerConfiguration> getDownloadPollers() {
        return downloadPollers;
    }

    public void setDownloadPollers(List<DownloadPollerConfiguration> downloadPollers) {
        this.downloadPollers = downloadPollers;
    }
}
