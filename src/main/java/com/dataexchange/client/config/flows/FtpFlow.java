package com.dataexchange.client.config.flows;

import com.dataexchange.client.config.model.DownloadPollerConfiguration;
import com.dataexchange.client.config.model.UploadPollerConfiguration;
import com.dataexchange.client.domain.util.ConnectionMonitorHelper;
import com.dataexchange.client.infrastructure.integration.RetryAdvice;
import com.dataexchange.client.infrastructure.integration.filters.FtpLastModifiedOlderThanFileFilter;
import com.dataexchange.client.infrastructure.integration.filters.FtpSemaphoreFileFilter;
import org.aopalliance.aop.Advice;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.dsl.FtpInboundChannelAdapterSpec;
import org.springframework.integration.ftp.dsl.FtpMessageHandlerSpec;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;

import static com.dataexchange.client.domain.util.LogHelper.*;
import static com.dataexchange.client.infrastructure.integration.FileAdapterHelper.*;
import static com.dataexchange.client.infrastructure.integration.PollerConfig.configureDownloadPoller;

@Component
public class FtpFlow {

    @Autowired
    private IntegrationFlowContext integrationFlowContext;
    @Autowired
    private ConnectionMonitorHelper connectionMonitorHelper;
    @Autowired
    private Advice moveFileAdvice;
    @Autowired
    private Advice pollerUpdateAdvice;

    public void downloadSetup(CachingSessionFactory ftpSessionFactory, DownloadPollerConfiguration config,
                              String connectionName, String username) {
        IntegrationFlowBuilder ftpFlowBuilder = IntegrationFlows
                .from(ftpInboundAdapter(ftpSessionFactory, config), conf -> conf.poller(configureDownloadPoller(config)
                        .maxMessagesPerPoll(-1)
                        .errorHandler(e -> connectionMonitorHelper.handleConnectionError(connectionName, e))
                        .advice(encrichLogsWithConnectionInfo(username, config.getOutputFolder()),
                                clearLogContext(),
                                connectionMonitorHelper.connectionSuccessAdvice(connectionName)
                        )
                ))
                .enrich(h -> h.header("poller_name", config.getName())
                        .header("connection_name", connectionName))
                .<File, Boolean>route(f -> hasSemaphoreSemantics(f, config),
                        semaphoreRouterAndOutboundAdapter(config, pollerUpdateAdvice));

        String beanName = "ftpDownloadFlow-" + config.getName();
        integrationFlowContext.registration(ftpFlowBuilder.get()).id(beanName).autoStartup(true).register();
    }

    public void uploadSetup(CachingSessionFactory ftpSessionFactory, UploadPollerConfiguration config,
                            String connectionName, String username) {
        StandardIntegrationFlow ftpFlow = IntegrationFlows
                .from(fileMessageSource(config.getInputFolder(), config.getRegexFilter()),
                        conf -> conf.poller(Pollers.fixedRate(10000).maxMessagesPerPoll(-1)))
                .enrichHeaders(h -> h.header("destination_folder", config.getProcessedFolder())
                        .header("poller_name", config.getName())
                        .header("connection_name", connectionName))
                .handle(ftpOutboundAdapter(ftpSessionFactory, config),
                        c -> c.advice(encrichLogsWithConnectionInfo(username, config.getRemoteOutputFolder()),
                                enrichLogsContextWithFileInfo(),
                                clearLogContext(),
                                RetryAdvice.retry(),
                                moveFileAdvice,
                                connectionMonitorHelper.connectionSuccessAdvice(connectionName),
                                connectionMonitorHelper.connectionErrorAdvice(connectionName)
                        )
                ).get();

        String beanName = "ftpUploadFlow-" + config.getName();
        integrationFlowContext.registration(ftpFlow).id(beanName).autoStartup(true).register();
    }

    private FtpInboundChannelAdapterSpec ftpInboundAdapter(CachingSessionFactory ftpSessionFactory, DownloadPollerConfiguration config) {
        CompositeFileListFilter<FTPFile> filters = buildFtpFilters(config);

        return Ftp.inboundAdapter(ftpSessionFactory)
                .localDirectory(new File(config.getDownloadFolder()))
                .deleteRemoteFiles(config.isDeleteRemoteFile())
                .filter(filters)
                .preserveTimestamp(true)
                .temporaryFileSuffix(".downloading")
                .remoteDirectory(config.getRemoteInputFolder());
    }

    private FtpMessageHandlerSpec ftpOutboundAdapter(CachingSessionFactory ftpSessionFactory, UploadPollerConfiguration config) {
        return Ftp.outboundAdapter(ftpSessionFactory)
                .autoCreateDirectory(true)
                .useTemporaryFileName(true)
                .temporaryFileSuffix(config.isUseTempPrefix() ? "" : ".uploading")
                .remoteDirectory(config.getRemoteOutputFolder());
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
}
