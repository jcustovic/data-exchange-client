package com.dataexchange.client.config.model;

import com.dataexchange.client.config.RemoteConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static com.dataexchange.client.config.model.FileType.*;

@ConfigurationProperties("app")
@Validated
public class MainConfiguration {

    @Autowired
    private RemoteConfiguration remoteConfiguration;

    @Valid
    private List<SftpPollerConfiguration> sftps = new ArrayList<>();
    @Valid
    private List<FtpPollerConfiguration> ftps = new ArrayList<>();

    @PostConstruct
    public void setUpFromRemote() {
        sftps.stream()
                .filter(SftpPollerConfiguration::hasRemoteConfig)
                .forEach(sftp -> {
                    RemoteConnectionConfiguration remoteConfig =
                            remoteConfiguration.getConnectionConfigByName(sftp.getRemoteConfigName());
                    setConnectionConfiguration(sftp, remoteConfig);
                    setRemoteFolders(sftp, remoteConfig);
                    sftp.setPrivateKey(new ByteArrayResource(remoteConfig.getPrivateKey().getBytes()));
                });

        ftps.stream()
                .filter(FtpPollerConfiguration::hasRemoteConfig)
                .forEach(ftp -> {
                    RemoteConnectionConfiguration remoteConfig =
                            remoteConfiguration.getConnectionConfigByName(ftp.getRemoteConfigName());
                    setConnectionConfiguration(ftp, remoteConfig);
                    setRemoteFolders(ftp, remoteConfig);
                });
    }

    private void setConnectionConfiguration(BasePollerConfiguration baseConfig,
                                            RemoteConnectionConfiguration remoteConfig) {
        baseConfig.setHost(remoteConfig.getHostname());
        baseConfig.setUsername(remoteConfig.getUsername());
        baseConfig.setPassword(remoteConfig.getPassword());
        baseConfig.setPort(remoteConfig.getPort());
    }

    private void setRemoteFolders(BasePollerConfiguration baseConfig, RemoteConnectionConfiguration remoteConfig) {
        for (DownloadPollerConfiguration downloadPollerConfig : baseConfig.getDownloadPollers()) {
            switch (downloadPollerConfig.getFileType()) {
                case JPG:
                    downloadPollerConfig.setRemoteInputFolder(remoteConfig.getRemoteFolders().get(JPG).getRemoteInputFolder());
                    break;
                case XML:
                    downloadPollerConfig.setRemoteInputFolder(remoteConfig.getRemoteFolders().get(XML).getRemoteInputFolder());
                    break;
                case TXT:
                    downloadPollerConfig.setRemoteInputFolder(remoteConfig.getRemoteFolders().get(TXT).getRemoteInputFolder());
                    break;
            }
        }
        for (UploadPollerConfiguration uploadPollerConfig : baseConfig.getUploadPollers()) {
            switch (uploadPollerConfig.getFileType()) {
                case PDF:
                    uploadPollerConfig.setRemoteOutputFolder(remoteConfig.getRemoteFolders().get(PDF).getRemoteOutputFolder());
                    break;
                case XML:
                    uploadPollerConfig.setRemoteOutputFolder(remoteConfig.getRemoteFolders().get(XML).getRemoteOutputFolder());
                    break;
                case TXT:
                    uploadPollerConfig.setRemoteOutputFolder(remoteConfig.getRemoteFolders().get(TXT).getRemoteOutputFolder());
                    break;
            }
        }
    }

    // Getters & Setters

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
