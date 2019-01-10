package com.dataexchange.client.config;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class FtpPollerConfiguration {

    @NotEmpty
    private String name;
    @NotEmpty
    private String host;
    private Integer port = 21;
    @NotEmpty
    private String username;
    private String password;
    @Valid
    private List<UploadPollerConfiguration> uploadPollers = new ArrayList<>();
    @Valid
    private List<DownloadPollerConfiguration> downloadPollers = new ArrayList<>();
    private String ftpParserDateFormat;

    // Getters & setters

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

    public String getFtpParserDateFormat() {
        return ftpParserDateFormat;
    }

    public void setFtpParserDateFormat(String ftpParserDateFormat) {
        this.ftpParserDateFormat = ftpParserDateFormat;
    }
}
