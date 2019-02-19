package com.dataexchange.client.config.flows;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.dataexchange.client.config.DynamicConfigurationCreator;
import com.dataexchange.client.config.model.S3Configuration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.amazonaws.services.s3.transfer.Transfer.TransferState.Completed;
import static com.dataexchange.client.infrastructure.integration.PollerConfig.secondsPoller;

@Component
public class S3Flow {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigurationCreator.class);

    @Autowired
    private IntegrationFlowContext integrationFlowContext;

    public void uploadSetup(String configName, S3Configuration config) {
        IntegrationFlowBuilder s3Flow = IntegrationFlows
                .from(Files.inboundAdapter(new File(config.getInputFolder())).preventDuplicates(true).autoCreateDirectory(true),
                        secondsPoller(30, 200))
                .enrichHeaders(h -> h.headerExpression(FileHeaders.ORIGINAL_FILE, "payload.path"))
                .handle(s3UploadAdapter(config))
                .handle(message -> {
                    Transfer payload = (Transfer) message.getPayload();
                    try {
                        payload.waitForCompletion();
                        if (Completed.equals(payload.getState())) {
                            FileUtils.deleteQuietly(new File((String) message.getHeaders().get(FileHeaders.ORIGINAL_FILE)));
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error in pushing to S3", e);
                    }
                });

        String beanName = "s3UploadFlow-" + configName;
        integrationFlowContext.registration(s3Flow.get()).id(beanName).autoStartup(true).register();
    }

    private S3MessageHandler s3UploadAdapter(S3Configuration config) {
        AmazonS3 amazonS3 = initAmazonS3(config);

        S3MessageHandler s3MessageHandler = new S3MessageHandler(amazonS3, config.getBucketName(), true);

        if (config.getServerSideEncryption()) {
            s3MessageHandler.setUploadMetadataProvider((metadata, message) -> {
                metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            });
        }

        return s3MessageHandler;
    }

    private AmazonS3 initAmazonS3(S3Configuration config) {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(config.getAwsAccessKey(), config.getAwsSecretKey());

        AWSSecurityTokenService sts = AWSSecurityTokenServiceClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .withRegion(config.getAwsRegion())
                .build();

        STSAssumeRoleSessionCredentialsProvider awsCredentialsProvider = new STSAssumeRoleSessionCredentialsProvider
                .Builder(config.getAwsRole(), "FileExchange")
                .withStsClient(sts)
                .build();

        ClientConfiguration clientConfiguration = new ClientConfiguration().withMaxErrorRetry(3);// retries

        return AmazonS3ClientBuilder.standard()
                .withCredentials(awsCredentialsProvider).withRegion(config.getAwsRegion())
                .withClientConfiguration(clientConfiguration)
                .build();
    }
}
