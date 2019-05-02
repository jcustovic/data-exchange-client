package com.dataexchange.client.config.flows;

import com.dataexchange.client.config.model.DownloadPollerConfiguration;
import com.dataexchange.client.config.model.UploadPollerConfiguration;
import com.dataexchange.client.domain.util.ConnectionMonitorHelper;
import com.dataexchange.client.infrastructure.integration.FileAdapterHelper;
import com.dataexchange.client.infrastructure.integration.RetryAdvice;
import com.dataexchange.client.infrastructure.integration.filters.SftpLastModifiedOlderThanFileFilter;
import com.dataexchange.client.infrastructure.integration.filters.SftpSemaphoreFileFilter;
import com.jcraft.jsch.ChannelSftp;
import org.aopalliance.aop.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.dsl.SftpInboundChannelAdapterSpec;
import org.springframework.integration.sftp.dsl.SftpMessageHandlerSpec;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;

import static com.dataexchange.client.domain.util.LogHelper.*;
import static com.dataexchange.client.infrastructure.integration.FileAdapterHelper.hasSemaphoreSemantics;
import static com.dataexchange.client.infrastructure.integration.FileAdapterHelper.semaphoreRouterAndOutboundAdapter;
import static com.dataexchange.client.infrastructure.integration.PollerConfig.configureDownloadPoller;
import static com.dataexchange.client.infrastructure.integration.PollerConfig.secondsPoller;

@Component
public class SftpFlow {

    @Autowired
    private IntegrationFlowContext integrationFlowContext;
    @Autowired
    private ConnectionMonitorHelper connectionMonitorHelper;
    @Autowired
    private Advice moveFileAdvice;

    public void downloadSetup(CachingSessionFactory sftpSessionFactory, DownloadPollerConfiguration config, String name, String username) {
        IntegrationFlowBuilder sftpFlowBuilder = IntegrationFlows
                .from(sftpInboundAdapter(sftpSessionFactory, config), conf -> conf.poller(configureDownloadPoller(config)
                        .maxMessagesPerPoll(100)
                        .errorHandler(e -> connectionMonitorHelper.handleConnectionError(name, e))
                        .advice(encrichLogsWithConnectionInfo(username, config.getOutputFolder()),
                                clearLogContext(),
                                connectionMonitorHelper.connectionSuccessAdvice(name)
                        )
                ))
                .<File, Boolean>route(f -> hasSemaphoreSemantics(f, config), semaphoreRouterAndOutboundAdapter(config));

        String beanName = "sftpDownloadFlow-" + config.getName();
        integrationFlowContext.registration(sftpFlowBuilder.get()).id(beanName).autoStartup(true).register();
    }

    public void uploadSetup(CachingSessionFactory sftpSessionFactory, UploadPollerConfiguration config, String name, String username) {
        StandardIntegrationFlow sftpFlow = IntegrationFlows
                .from(FileAdapterHelper.fileMessageSource(config.getInputFolder(), config.getRegexFilter()), secondsPoller(10, 100))
                .enrichHeaders(h -> h.header("destination_folder", config.getProcessedFolder()))
                .handle(sftpOutboundAdapter(sftpSessionFactory, config),
                        conf -> conf.advice(
                                encrichLogsWithConnectionInfo(username, config.getRemoteOutputFolder()),
                                enrichLogsContextWithFileInfo(),
                                clearLogContext(),
                                RetryAdvice.retry(),
                                moveFileAdvice,
                                connectionMonitorHelper.connectionSuccessAdvice(name),
                                connectionMonitorHelper.connectionErrorAdvice(name)
                        )
                ).get();

        String beanName = "sftpUploadFlow-" + config.getName();
        integrationFlowContext.registration(sftpFlow).id(beanName).autoStartup(true).register();
    }

    private SftpInboundChannelAdapterSpec sftpInboundAdapter(CachingSessionFactory sftpSessionFactory, DownloadPollerConfiguration config) {
        CompositeFileListFilter<ChannelSftp.LsEntry> filters = buildSftpFilters(config);

        return Sftp.inboundAdapter(sftpSessionFactory)
                .localDirectory(new File(config.getDownloadFolder()))
                .deleteRemoteFiles(config.isDeleteRemoteFile())
                .filter(filters)
                .preserveTimestamp(true)
                .temporaryFileSuffix(".downloading")
                .remoteDirectory(config.getRemoteInputFolder());
    }

    private SftpMessageHandlerSpec sftpOutboundAdapter(CachingSessionFactory sftpSessionFactory, UploadPollerConfiguration config) {
        return Sftp.outboundAdapter(sftpSessionFactory)
                .autoCreateDirectory(true)
                .useTemporaryFileName(true)
                .temporaryFileSuffix(".uploading")
                .remoteDirectory(config.getRemoteOutputFolder());
    }

    private CompositeFileListFilter<ChannelSftp.LsEntry> buildSftpFilters(DownloadPollerConfiguration config) {
        CompositeFileListFilter<ChannelSftp.LsEntry> chainFileListFilter = new ChainFileListFilter<>();
        if (StringUtils.hasText(config.getSemaphoreFileSuffix())) {
            chainFileListFilter.addFilter(new SftpSemaphoreFileFilter(config.getSemaphoreFileSuffix()));
        }
        if (config.getModifiedDateAfterMinutes() != null) {
            chainFileListFilter.addFilter(new SftpLastModifiedOlderThanFileFilter(config.getModifiedDateAfterMinutes()));
        }
        if (!config.isDeleteRemoteFile()) {
            chainFileListFilter.addFilter(new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), ""));
        }
        if (StringUtils.hasText(config.getRegexFilter())) {
            chainFileListFilter.addFilter(new SftpRegexPatternFileListFilter(config.getRegexFilter()));
        }

        return chainFileListFilter;
    }
}
