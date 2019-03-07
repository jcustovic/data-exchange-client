package com.dataexchange.client.config.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("app")
public class MainConfiguration {

    @Valid
    private List<SftpPollerConfiguration> sftps = new ArrayList<>();
    @Valid
    private List<FtpPollerConfiguration> ftps = new ArrayList<>();

    public List<SftpPollerConfiguration> getSftps() {
        return sftps;
    }

    public void setSftps(List<SftpPollerConfiguration> sftps) {
        this.sftps = sftps;
    }

    public List<FtpPollerConfiguration> getFtps() {
        return ftps;
    }

    public void setFtps(List<FtpPollerConfiguration> ftps) {
        this.ftps = ftps;
    }
}
