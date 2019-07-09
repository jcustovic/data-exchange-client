package com.dataexchange.client.config.flows;

import com.dataexchange.client.config.model.ZipConfiguration;
import com.dataexchange.client.domain.ZipFilesTransformer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.dsl.AggregatorSpec;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class ZipFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipFilesTransformer.class);

    @Autowired
    private IntegrationFlowContext integrationFlowContext;
    @Autowired
    private ZipFilesTransformer zipFilesTransformer;

    public void setup(String inputFolder, String configName, ZipConfiguration config) {

        IntegrationFlowBuilder zipFlow = IntegrationFlows
                .from(inboundAdapter(inputFolder),
                        conf -> conf.poller(Pollers.fixedRate(10, SECONDS).maxMessagesPerPoll(config.getMinItemsToZip())))
                .enrichHeaders(h -> h.header("file_type", "jpg"))
                .aggregate(customAggregator(config.getMinItemsToZip()))
                .channel("after_aggregation-channel")
                .enrichHeaders(h -> h.header("filename_pattern", config.getPattern())
                        .header("input_folder", inputFolder))
                .channel("zip_files-channel")
                .transform(zipFilesTransformer)
                .channel("move_zipped_file-channel")
                .handle(moveFileHandler(config.getOutputFolder()));

        String beanName = "zipFlow-" + configName;
        integrationFlowContext.registration(zipFlow.get()).id(beanName).autoStartup(true).register();
    }

    private FileInboundChannelAdapterSpec inboundAdapter(String inputFolder) {
        return Files.inboundAdapter(new File(inputFolder)).preventDuplicates(true);
    }

    private Consumer<AggregatorSpec> customAggregator(Integer minItemsToZip) {
        return aggregatorSpec -> aggregatorSpec
                .correlationExpression("payload.getParent()")
                .releaseStrategy(group -> group.getMessages().size() == minItemsToZip)
                .expireGroupsUponCompletion(true);
    }

    private MessageHandler moveFileHandler(String destinationFolder) {
        return message -> {
            File zippedFile = (File) message.getPayload();
            try {
                FileUtils.moveFile(zippedFile, new File(destinationFolder + "/" + zippedFile.getName()));
            } catch (IOException e) {
                String zipName = (String) message.getHeaders().get("zip_filename");
                LOGGER.error("Moving of file failed for file: " + zipName, e);
            }
        };
    }

}
