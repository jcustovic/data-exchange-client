package com.dataexchange.client.config;

import com.dataexchange.client.config.flows.FtpFlow;
import com.dataexchange.client.config.flows.S3Flow;
import com.dataexchange.client.config.flows.SftpFlow;
import com.dataexchange.client.config.flows.ZipFlow;
import com.dataexchange.client.config.model.*;
import com.dataexchange.client.domain.ConnectionMonitor;
import com.dataexchange.client.domain.model.PollerStatus;
import com.dataexchange.client.infrastructure.integration.file.LoggingSessionFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DynamicConfigurationCreator {

    @Autowired
    private MainConfiguration configuration;
    @Autowired
    private ConnectionMonitor connectionMonitor;
    @Autowired
    private S3Flow s3Flow;
    @Autowired
    private ZipFlow zipFlow;
    @Autowired
    private SftpFlow sftpFlow;
    @Autowired
    private FtpFlow ftpFlow;
    @Autowired(required = false)
    private RestHighLevelClient restHighLevelClient;
    @Value("${app.es.index_pattern:#{null}}")
    private String indexPattern;

    @PostConstruct
    public void setup() {
        for (SftpPollerConfiguration sftp : configuration.getSftps()) {
            Map<String, PollerStatus> pollerStatusMap = buildPollerStatus(sftp.getDownloadPollers(),
                    sftp.getUploadPollers());
            connectionMonitor.register(sftp.getName(), pollerStatusMap);
            CachingSessionFactory sftpSessionFactory = sftpSessionFactory(sftp);
            createAndRegisterUploadSftpFlowBeans(sftpSessionFactory, sftp);
            createAndRegisterDownloadSftpFlowBeans(sftpSessionFactory, sftp);
        }

        for (FtpPollerConfiguration ftp : configuration.getFtps()) {
            Map<String, PollerStatus> pollerStatusMap = buildPollerStatus(ftp.getDownloadPollers(),
                    ftp.getUploadPollers());
            connectionMonitor.register(ftp.getName(), pollerStatusMap);
            CachingSessionFactory ftpSessionFactory = ftpSessionFactory(ftp);
            createAndRegisterUploadFtpFlowBeans(ftpSessionFactory, ftp);
            createAndRegisterDownloadFtpFlowBeans(ftpSessionFactory, ftp);
        }
    }

    private Map<String, PollerStatus> buildPollerStatus(List<DownloadPollerConfiguration> downloadPollers,
                                                        List<UploadPollerConfiguration> uploadPollers) {
        Map<String, PollerStatus> uploadPollerStatus = uploadPollers.stream().collect(
                Collectors.toMap(UploadPollerConfiguration::getName, dp -> new PollerStatus(PollerStatus.PollerStatusDirection.UPLOAD)));

        Map<String, PollerStatus> downloadPollerStatus = downloadPollers.stream().collect(
                Collectors.toMap(DownloadPollerConfiguration::getName, dp -> new PollerStatus(PollerStatus.PollerStatusDirection.DOWNLOAD)));
        downloadPollerStatus.putAll(uploadPollerStatus);

        return downloadPollerStatus;
    }

    private void createAndRegisterUploadSftpFlowBeans(CachingSessionFactory sftpSessionFactory, SftpPollerConfiguration sftpConfig) {
        sftpConfig.getUploadPollers()
                .forEach(config -> sftpFlow.uploadSetup(sftpSessionFactory, config, sftpConfig.getName(), sftpConfig.getUsername()));
    }

    private void createAndRegisterDownloadSftpFlowBeans(CachingSessionFactory sftpSessionFactory, SftpPollerConfiguration sftpConfig) {
        for (DownloadPollerConfiguration pollerConfig : sftpConfig.getDownloadPollers()) {
            sftpFlow.downloadSetup(sftpSessionFactory, pollerConfig, sftpConfig.getName(), sftpConfig.getUsername());

            if (pollerConfig.getS3Configuration() != null) {
                s3Flow.uploadSetup(pollerConfig.getName(), pollerConfig.getS3Configuration());
            }
            if (pollerConfig.getZipConfiguration() != null) {
                zipFlow.setup(pollerConfig.getOutputFolder(), pollerConfig.getName(), pollerConfig.getZipConfiguration());
            }
        }
    }

    private void createAndRegisterUploadFtpFlowBeans(CachingSessionFactory ftpSessionFactory, FtpPollerConfiguration ftpConfig) {
        ftpConfig.getUploadPollers()
                .forEach(config -> ftpFlow.uploadSetup(ftpSessionFactory, config, ftpConfig.getName(), ftpConfig.getUsername()));
    }

    private void createAndRegisterDownloadFtpFlowBeans(CachingSessionFactory ftpSessionFactory, FtpPollerConfiguration ftpConfig) {
        for (DownloadPollerConfiguration pollerConfig : ftpConfig.getDownloadPollers()) {
            ftpFlow.downloadSetup(ftpSessionFactory, pollerConfig, ftpConfig.getName(), ftpConfig.getUsername());

            if (pollerConfig.getS3Configuration() != null) {
                s3Flow.uploadSetup(pollerConfig.getName(), pollerConfig.getS3Configuration());
            }
            if (pollerConfig.getZipConfiguration() != null) {
                zipFlow.setup(pollerConfig.getOutputFolder(), pollerConfig.getName(), pollerConfig.getZipConfiguration());
            }
        }
    }

    private CachingSessionFactory sftpSessionFactory(SftpPollerConfiguration sftpPollerConfiguration) {
        DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
        sftpSessionFactory.setHost(sftpPollerConfiguration.getHost());
        sftpSessionFactory.setUser(sftpPollerConfiguration.getUsername());
        sftpSessionFactory.setPassword(sftpPollerConfiguration.getPassword());
        sftpSessionFactory.setPrivateKey(sftpPollerConfiguration.getPrivateKey());
        sftpSessionFactory.setPrivateKeyPassphrase(sftpPollerConfiguration.getPrivateKeyPassphrase());
        sftpSessionFactory.setPort(sftpPollerConfiguration.getPort());
        sftpSessionFactory.setServerAliveInterval(30_000);
        sftpSessionFactory.setTimeout(30_000);
        sftpSessionFactory.setAllowUnknownKeys(true);

        return createSessionFactory(sftpSessionFactory, 1, sftpPollerConfiguration.getUsername(),
                sftpPollerConfiguration.getHost());
    }

    private CachingSessionFactory ftpSessionFactory(FtpPollerConfiguration ftpPollerConfiguration) {
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setHost(ftpPollerConfiguration.getHost());
        ftpSessionFactory.setUsername(ftpPollerConfiguration.getUsername());
        ftpSessionFactory.setPassword(ftpPollerConfiguration.getPassword());
        ftpSessionFactory.setPort(ftpPollerConfiguration.getPort());
        ftpSessionFactory.setDefaultTimeout(40_000);
        ftpSessionFactory.setConnectTimeout(30_000);
        ftpSessionFactory.setDataTimeout(30_000);
        ftpSessionFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);

        if (StringUtils.hasText(ftpPollerConfiguration.getFtpParserDateFormat())) {
            FTPClientConfig ftpClientConfig = new FTPClientConfig("");
            ftpClientConfig.setDefaultDateFormatStr(ftpPollerConfiguration.getFtpParserDateFormat());
            ftpSessionFactory.setConfig(ftpClientConfig);
        }

        return createSessionFactory(ftpSessionFactory, 5, ftpPollerConfiguration.getUsername(),
                ftpPollerConfiguration.getHost());
    }

    private CachingSessionFactory createSessionFactory(SessionFactory sessionFactory, int sessionCacheSize,
                                                       String connectionUsername, String connectionHost) {
        CachingSessionFactory cachingFactory;

        if (indexPattern == null) {
            cachingFactory = new CachingSessionFactory(sessionFactory, sessionCacheSize);
        } else {
            cachingFactory = new LoggingSessionFactory(restHighLevelClient, indexPattern, sessionFactory,
                    sessionCacheSize, connectionUsername, connectionHost);
        }
        cachingFactory.setSessionWaitTimeout(60_000);
        cachingFactory.setTestSession(true);

        return cachingFactory;
    }
}
