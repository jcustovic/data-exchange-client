package com.dataexchange.client.infrastructure.integration;

import com.dataexchange.client.config.model.DownloadPollerConfiguration;
import org.aopalliance.aop.Advice;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.RouterSpec;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.dsl.FileWritingMessageHandlerSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.filters.*;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.function.Consumer;

import static com.dataexchange.client.domain.util.LogHelper.enrichLogsContextWithFileInfo;

public final class FileAdapterHelper {

    public static MessageSource<File> fileMessageSource(String path, String regexFilter) {
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

    public static FileWritingMessageHandlerSpec defaultFileOutboundAdapter(String outputFolder, String fileNameExpression) {
        FileWritingMessageHandlerSpec outboundAdapter = Files.outboundAdapter(new File(outputFolder))
                .fileExistsMode(FileExistsMode.REPLACE)
                .deleteSourceFiles(true);

        if (StringUtils.hasText(fileNameExpression)) {
            outboundAdapter.fileNameExpression(fileNameExpression);
        }

        return outboundAdapter;
    }

    public static Consumer<RouterSpec<Boolean, MethodInvokingRouter>> semaphoreRouterAndOutboundAdapter(
            DownloadPollerConfiguration config, Advice pollerUpdateAdvice) {
        return mapping -> mapping.id(config.getName() + "-isSemaphoreFileDecider")
                .subFlowMapping(true, sf -> sf.handle(message -> {
                            if (message.getPayload() instanceof File) {
                                File semFile = (File) message.getPayload();
                                semFile.delete();
                            }
                        }, a -> a.id(config.getName() + "-deleteSemaphoreFile").advice(enrichLogsContextWithFileInfo()))
                )
                .subFlowMapping(false, sf -> sf.handle(defaultFileOutboundAdapter(config.getOutputFolder(),
                        config.getOutputFileNameExpression()),
                        a -> a.id(config.getName() + "-moveFileToOutputFolder")
                                .advice(RetryAdvice.retry(), enrichLogsContextWithFileInfo(), pollerUpdateAdvice)
                ));
    }

    public static boolean hasSemaphoreSemantics(File file, DownloadPollerConfiguration config) {
        return StringUtils.hasText(config.getSemaphoreFileSuffix()) && file.getName().endsWith(config.getSemaphoreFileSuffix());
    }
}
