package com.dataexchange.client.config;

import com.dataexchange.client.config.flows.FtpFlow;
import com.dataexchange.client.config.flows.S3Flow;
import com.dataexchange.client.config.flows.SftpFlow;
import com.dataexchange.client.config.flows.ZipFlow;
import com.dataexchange.client.config.model.DownloadPollerConfiguration;
import com.dataexchange.client.config.model.FtpPollerConfiguration;
import com.dataexchange.client.config.model.MainConfiguration;
import com.dataexchange.client.config.model.SftpPollerConfiguration;
import com.dataexchange.client.domain.ConnectionMonitor;
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
            connectionMonitor.register(sftp.getName());
            CachingSessionFactory sftpSessionFactory = sftpSessionFactory(sftp);
            createAndRegisterUploadSftpFlowBeans(sftpSessionFactory, sftp);
            createAndRegisterDownloadSftpFlowBeans(sftpSessionFactory, sftp);
        }

        for (FtpPollerConfiguration ftp : configuration.getFtps()) {
            connectionMonitor.register(ftp.getName());
            CachingSessionFactory ftpSessionFactory = ftpSessionFactory(ftp);
            createAndRegisterUploadFtpFlowBeans(ftpSessionFactory, ftp);
            createAndRegisterDownloadFtpFlowBeans(ftpSessionFactory, ftp);
        }
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

        return createSessionFactory(sftpSessionFactory, 5, sftpPollerConfiguration.getUsername(),
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
        if (indexPattern == null) {
            return new CachingSessionFactory(sessionFactory, sessionCacheSize);
        } else {
            return new LoggingSessionFactory(restHighLevelClient, indexPattern, sessionFactory, sessionCacheSize,
                    connectionUsername, connectionHost);
        }
    }
}
