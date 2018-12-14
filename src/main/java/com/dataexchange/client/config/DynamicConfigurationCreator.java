package com.dataexchange.client.config;

import com.dataexchange.client.domain.ConnectionMonitor;
import com.dataexchange.client.infrastructure.ConnectionMonitorThrowsAdvice;
import com.dataexchange.client.infrastructure.integration.filters.FtpLastModifiedOlderThanFileFilter;
import com.dataexchange.client.infrastructure.integration.filters.FtpSemaphoreFileFilter;
import com.dataexchange.client.infrastructure.integration.filters.SftpLastModifiedOlderThanFileFilter;
import com.dataexchange.client.infrastructure.integration.filters.SftpSemaphoreFileFilter;
import com.jcraft.jsch.ChannelSftp;
import org.aopalliance.aop.Advice;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.*;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.dsl.FileWritingMessageHandlerSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.filters.*;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.dsl.FtpInboundChannelAdapterSpec;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.dsl.SftpInboundChannelAdapterSpec;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;

@Component
public class DynamicConfigurationCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigurationCreator.class);

    @Autowired
    private MainConfiguration configuration;
    @Autowired
    private IntegrationFlowContext integrationFlowContext;
    @Autowired
    private Advice moveFileAdvice;
    @Autowired
    private ConnectionMonitor connectionMonitor;

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

    private void createAndRegisterUploadSftpFlowBeans(CachingSessionFactory sftpSessionFactory,
                                                      SftpPollerConfiguration sftpConfig) {
        for (UploadPollerConfiguration pollerConfig : sftpConfig.getUploadPollers()) {
            StandardIntegrationFlow sftpFlow = IntegrationFlows
                    .from(fileMessageSource(pollerConfig.getInputFolder(), pollerConfig.getRegexFilter()),
                            conf -> conf.poller(Pollers.fixedRate(10000).maxMessagesPerPoll(100)))
                    .enrichHeaders(h -> h.header("destination_folder", pollerConfig.getProcessedFolder()))
                    .handle(Sftp.outboundAdapter(sftpSessionFactory)
                                    .autoCreateDirectory(true)
                                    .useTemporaryFileName(true)
                                    .temporaryFileSuffix(".uploading")
                                    .remoteDirectory(pollerConfig.getRemoteOutputFolder()),
                            conf -> conf.advice(retryAdvice(), moveFileAdvice, connectionSuccessAdvice(sftpConfig.getName()),
                                    connectionErrorAdvice(sftpConfig.getName())
                            )
                    ).get();

            String beanName = "sftpUploadFlow-" + pollerConfig.getName();
            integrationFlowContext.registration(sftpFlow).id(beanName).autoStartup(true).register();
        }
    }

    private void createAndRegisterDownloadSftpFlowBeans(CachingSessionFactory sftpSessionFactory,
                                                        SftpPollerConfiguration sftpConfig) {
        for (DownloadPollerConfiguration pollerConfig : sftpConfig.getDownloadPollers()) {
            CompositeFileListFilter<ChannelSftp.LsEntry> filters = buildSftpFilters(pollerConfig);
            SftpInboundChannelAdapterSpec sftpInboundChannelAdapter = Sftp.inboundAdapter(sftpSessionFactory)
                    .localDirectory(new File(pollerConfig.getDownloadFolder()))
                    .deleteRemoteFiles(pollerConfig.isDeleteRemoteFile())
                    .filter(filters)
                    .preserveTimestamp(true)
                    .temporaryFileSuffix(".downloading")
                    .remoteDirectory(pollerConfig.getRemoteInputFolder());

            IntegrationFlowBuilder sftpFlowBuilder = IntegrationFlows
                    .from(sftpInboundChannelAdapter, conf -> conf.poller(configurePoller(pollerConfig)
                            .maxMessagesPerPoll(100)
                            .errorHandler(e -> handleConnectionError(sftpConfig.getName(), e))
                            .advice(connectionSuccessAdvice(sftpConfig.getName()))
                    ));
            sftpFlowBuilder = configureDownloadFlowHandle(sftpFlowBuilder, pollerConfig);

            String beanName = "sftpDownloadFlow-" + pollerConfig.getName();
            integrationFlowContext.registration(sftpFlowBuilder.get()).id(beanName).autoStartup(true).register();
        }
    }

    private CompositeFileListFilter<ChannelSftp.LsEntry> buildSftpFilters(DownloadPollerConfiguration pollerConfig) {
        CompositeFileListFilter<ChannelSftp.LsEntry> chainFileListFilter = new ChainFileListFilter<>();
        if (StringUtils.hasText(pollerConfig.getSemaphoreFileSuffix())) {
            chainFileListFilter.addFilter(new SftpSemaphoreFileFilter(pollerConfig.getSemaphoreFileSuffix()));
        }
        if (pollerConfig.getModifiedDateAfterMinutes() != null) {
            chainFileListFilter.addFilter(new SftpLastModifiedOlderThanFileFilter(pollerConfig.getModifiedDateAfterMinutes()));
        }
        if (!pollerConfig.isDeleteRemoteFile()) {
            chainFileListFilter.addFilter(new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), ""));
        }
        if (StringUtils.hasText(pollerConfig.getRegexFilter())) {
            chainFileListFilter.addFilter(new SftpRegexPatternFileListFilter(pollerConfig.getRegexFilter()));
        }

        return chainFileListFilter;
    }

    private void createAndRegisterUploadFtpFlowBeans(CachingSessionFactory ftpSessionFactory,
                                                     FtpPollerConfiguration ftpConfig) {
        for (UploadPollerConfiguration pollerConfig : ftpConfig.getUploadPollers()) {
            StandardIntegrationFlow ftpFlow = IntegrationFlows
                    .from(fileMessageSource(pollerConfig.getInputFolder(), pollerConfig.getRegexFilter()),
                            conf -> conf.poller(Pollers.fixedRate(10000).maxMessagesPerPoll(100)))
                    .enrichHeaders(h -> h.header("destination_folder", pollerConfig.getProcessedFolder()))
                    .handle(Ftp.outboundAdapter(ftpSessionFactory)
                                    .autoCreateDirectory(true)
                                    .useTemporaryFileName(true)
                                    .temporaryFileSuffix(".uploading")
                                    .remoteDirectory(pollerConfig.getRemoteOutputFolder()),
                            c -> c.advice(retryAdvice(), moveFileAdvice, connectionSuccessAdvice(ftpConfig.getName()),
                                    connectionErrorAdvice(ftpConfig.getName())
                            )
                    ).get();

            String beanName = "ftpUploadFlow-" + pollerConfig.getName();
            integrationFlowContext.registration(ftpFlow).id(beanName).autoStartup(true).register();
        }
    }

    private void createAndRegisterDownloadFtpFlowBeans(CachingSessionFactory ftpSessionFactory,
                                                       FtpPollerConfiguration ftpConfig) {
        for (DownloadPollerConfiguration pollerConfig : ftpConfig.getDownloadPollers()) {
            CompositeFileListFilter<FTPFile> filters = buildFtpFilters(pollerConfig);
            FtpInboundChannelAdapterSpec ftpInboundChannelAdapter = Ftp.inboundAdapter(ftpSessionFactory)
                    .localDirectory(new File(pollerConfig.getDownloadFolder()))
                    .deleteRemoteFiles(pollerConfig.isDeleteRemoteFile())
                    .filter(filters)
                    .preserveTimestamp(true)
                    .temporaryFileSuffix(".downloading")
                    .remoteDirectory(pollerConfig.getRemoteInputFolder());

            IntegrationFlowBuilder ftpFlowBuilder = IntegrationFlows
                    .from(ftpInboundChannelAdapter, conf -> conf.poller(configurePoller(pollerConfig)
                            .maxMessagesPerPoll(100)
                            .errorHandler(e -> handleConnectionError(ftpConfig.getName(), e))
                            .advice(connectionSuccessAdvice(ftpConfig.getName()))
                    ));
            ftpFlowBuilder = configureDownloadFlowHandle(ftpFlowBuilder, pollerConfig);

            String beanName = "ftpDownloadFlow-" + pollerConfig.getName();
            integrationFlowContext.registration(ftpFlowBuilder.get()).id(beanName).autoStartup(true).register();
        }
    }

    private PollerSpec configurePoller(DownloadPollerConfiguration pollerConfig) {
        if (StringUtils.hasText(pollerConfig.getPollCron())) {
            return Pollers.cron(pollerConfig.getPollCron());
        } else {
            return Pollers.fixedRate(pollerConfig.getPollIntervalMilliseconds());
        }
    }

    private IntegrationFlowBuilder configureDownloadFlowHandle(IntegrationFlowBuilder flowBuilder,
                                                               DownloadPollerConfiguration pollerConfig) {
        if (StringUtils.hasText(pollerConfig.getSemaphoreFileSuffix())) {
            return flowBuilder.<File, Boolean>route(f -> f.getName().endsWith(pollerConfig.getSemaphoreFileSuffix()), mapping ->
                    mapping
                            .subFlowMapping(true, sf -> sf.handle(message -> {
                                        if (message.getPayload() instanceof File) {
                                            File semFile = (File) message.getPayload();
                                            boolean deleteResult = semFile.delete();
                                            LOGGER.debug("Deleting {} ({})", semFile.getAbsolutePath(), deleteResult);
                                        }
                                    })
                            )
                            .subFlowMapping(false, sf -> sf.handle(buildDefaultFileOutboundAdapter(pollerConfig.getOutputFolder(),
                                    pollerConfig.getOutputFileNameExpression()), a -> a.advice(retryAdvice())
                            ))
            );
        } else {
            return flowBuilder.handle(buildDefaultFileOutboundAdapter(pollerConfig.getOutputFolder(),
                    pollerConfig.getOutputFileNameExpression()), a -> a.advice(retryAdvice()));
        }
    }

    private FileWritingMessageHandlerSpec buildDefaultFileOutboundAdapter(String outputFolder, String fileNameExpression) {
        FileWritingMessageHandlerSpec outbountAdapter = Files.outboundAdapter(new File(outputFolder))
                .fileExistsMode(FileExistsMode.REPLACE)
                .deleteSourceFiles(true);

        if (StringUtils.hasText(fileNameExpression)) {
            outbountAdapter.fileNameExpression(fileNameExpression);
        }

        return outbountAdapter;
    }

    private CompositeFileListFilter<FTPFile> buildFtpFilters(DownloadPollerConfiguration pollerConfig) {
        CompositeFileListFilter<FTPFile> chainFileListFilter = new ChainFileListFilter<>();
        if (StringUtils.hasText(pollerConfig.getSemaphoreFileSuffix())) {
            chainFileListFilter.addFilter(new FtpSemaphoreFileFilter(pollerConfig.getSemaphoreFileSuffix()));
        }
        if (pollerConfig.getModifiedDateAfterMinutes() != null) {
            chainFileListFilter.addFilter(new FtpLastModifiedOlderThanFileFilter(pollerConfig.getModifiedDateAfterMinutes()));
        }
        if (!pollerConfig.isDeleteRemoteFile()) {
            chainFileListFilter.addFilter(new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), ""));
        }
        if (StringUtils.hasText(pollerConfig.getRegexFilter())) {
            chainFileListFilter.addFilter(new FtpRegexPatternFileListFilter(pollerConfig.getRegexFilter()));
        }

        return chainFileListFilter;
    }

    private AfterReturningAdvice connectionSuccessAdvice(final String connectionName) {
        return (returnValue, method, args, target) -> handleConnectionUp(connectionName);
    }

    private void handleConnectionError(String name, Throwable e) {
        connectionMonitor.down(name, e);
    }

    private void handleConnectionUp(String name) {
        connectionMonitor.up(name);
    }

    private MessageSource<File> fileMessageSource(String path, String regexFilter) {
        FileReadingMessageSource source = new FileReadingMessageSource();
        source.setDirectory(new File(path));
        source.setAutoCreateDirectory(true);

        CompositeFileListFilter<File> compositeFilter = new CompositeFileListFilter<>();
        compositeFilter.addFilter(new AbstractFileListFilter<File>() {
            @Override
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
        compositeFilter.addFilter(new IgnoreHiddenFileListFilter());
        if (StringUtils.hasText(regexFilter)) {
            compositeFilter.addFilter(new RegexPatternFileListFilter(regexFilter));
        }
        compositeFilter.addFilter(new AcceptOnceFileListFilter<>());
        source.setFilter(compositeFilter);

        return source;
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

        return new CachingSessionFactory(sftpSessionFactory, 5);
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

        return new CachingSessionFactory(ftpSessionFactory, 5);
    }

    private ThrowsAdvice connectionErrorAdvice(String connectionName) {
        return new ConnectionMonitorThrowsAdvice(connectionName, connectionMonitor);
    }

    private Advice retryAdvice() {
        RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(10_000);
        exponentialBackOffPolicy.setMaxInterval(60_000);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
        retryTemplate.setRetryPolicy(new AlwaysRetryPolicy());
        advice.setRetryTemplate(retryTemplate);

        return advice;
    }
}
