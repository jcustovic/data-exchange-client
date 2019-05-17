package com.dataexchange.client.config.flows;

import com.dataexchange.client.config.model.ZipConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.dsl.AggregatorSpec;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.zip.ZipHeaders;
import org.springframework.integration.zip.transformer.ZipTransformer;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import static com.dataexchange.client.infrastructure.integration.PollerConfig.minutesPoller;
import static java.time.format.DateTimeFormatter.ofPattern;

@Component
public class ZipFlow {

    @Autowired
    private IntegrationFlowContext integrationFlowContext;

    public void setup(String inputFolder, String configName, ZipConfiguration config) {

        String zipFileName = config.getPattern().replace("%s", LocalDateTime.now().format(ofPattern("YYYY-MM-dd_HH-mm-ss")));

        StandardIntegrationFlow zipFlow = IntegrationFlows
                .from(inboundAdapter(inputFolder), minutesPoller(1))
                .aggregate(customAggregator(config.getMinItemsToZip()))
                .enrichHeaders(h -> h
                        .header(ZipHeaders.ZIP_ENTRY_FILE_NAME, config.getPattern())
                        .header(FileHeaders.FILENAME, zipFileName))
                .transform(zipTransformer())
                .handle(Files.outboundAdapter(new File(config.getOutputFolder())).deleteSourceFiles(true))
                .get();

        String beanName = "zipFlow-" + configName;
        integrationFlowContext.registration(zipFlow).id(beanName).autoStartup(true).register();
    }

    private FileInboundChannelAdapterSpec inboundAdapter(String inputFolder) {
        return Files.inboundAdapter(new File(inputFolder)).preventDuplicates(true).scanEachPoll(true);
    }

    private Consumer<AggregatorSpec> customAggregator(Integer minItemsToZip) {
        return aggregatorSpec -> aggregatorSpec
                .correlationExpression("payload.getParent()")
                .releaseExpression("size() > " + minItemsToZip)
                .groupTimeout(10_000)
                .sendPartialResultOnExpiry(true);
    }

    private ZipTransformer zipTransformer() {
        ZipTransformer zipTransformer = new ZipTransformer();
        zipTransformer.setDeleteFiles(true);

        return zipTransformer;
    }
}
