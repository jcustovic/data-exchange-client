package com.dataexchange.client.config.flows;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.dataexchange.client.config.DynamicConfigurationCreator;
import com.dataexchange.client.config.model.S3Configuration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static com.dataexchange.client.domain.util.LogHelper.clearLogContext;
import static com.dataexchange.client.domain.util.LogHelper.encrichLogsWithConnectionInfo;

@Component
public class S3Flow {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigurationCreator.class);

    @Autowired
    private IntegrationFlowContext integrationFlowContext;

    public void uploadSetup(String configName, S3Configuration config, String username) {
        IntegrationFlowBuilder s3Flow = IntegrationFlows
                .from(Files.inboundAdapter(new File(config.getInputFolder())).preventDuplicates(true).autoCreateDirectory(true),
                        conf -> conf.poller(Pollers.fixedRate(2000).maxMessagesPerPoll(100)))
                .enrichHeaders(h -> h.headerExpression(FileHeaders.ORIGINAL_FILE, "payload.path"))
                .handle((GenericHandler<File>) (payload, headers) -> {
                    String filename = (String) headers.get(FileHeaders.FILENAME);
                    String bucketName = resolveBucketName(config.getBucketName(), filename);

                    TransferManager tf = TransferManagerBuilder.standard().withS3Client(initS3Client(config)).build();

                    ObjectMetadata objectMetadata = new ObjectMetadata();
                    if (config.getServerSideEncryption()) {
                        objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
                    }

                    try {
                        return tf.upload(bucketName, filename, new FileInputStream(payload), objectMetadata);
                    } catch (FileNotFoundException e) {
                        LOGGER.error("Upload to S3 has failed for file {}", filename, e);
                        return null;
                    }
                }, conf -> conf.advice(encrichLogsWithConnectionInfo(username, config.getInputFolder()), clearLogContext()))
                .channel("s3UploadFlow-channel")
                .<Upload, Boolean>route(this::doUpload, mapping -> mapping
                        .subFlowMapping(true, sf -> sf.handle(message -> {
                                    File file = (File) message.getHeaders().get(FileHeaders.ORIGINAL_FILE);
                                    LOGGER.info("Deleting file: {}", file.getPath());
                                    FileUtils.deleteQuietly(file);
                                }, conf -> conf.advice(encrichLogsWithConnectionInfo(username, config.getInputFolder())))
                        )
                        .subFlowMapping(false, sf -> sf.delay("s3-error-delayer",
                                c -> c.defaultDelay(600_000)).channel("s3UploadFlow-channel")
                        ));

        String beanName = "s3UploadFlow-" + configName;
        integrationFlowContext.registration(s3Flow.get()).id(beanName).autoStartup(true).register();
    }

    private String resolveBucketName(String bucketName, String filename) {
        if (filename.endsWith(".zip") || filename.endsWith(".tar")) {
            return bucketName + "/zip";
        }

        return bucketName + "/jpg/" + filename.substring(0, 6);
    }

    private boolean doUpload(Upload upload) {
        try {
            upload.waitForCompletion();
            return true;
        } catch (AmazonClientException | InterruptedException e) {
            LOGGER.error("Uploading to S3 failed. ", e);
            return false;
        }
    }

    private AmazonS3 initS3Client(S3Configuration config) {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(config.getAwsAccessKey(), config.getAwsSecretKey());

        AWSSecurityTokenService sts = AWSSecurityTokenServiceClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .withRegion(config.getAwsRegion())
                .build();

        STSAssumeRoleSessionCredentialsProvider awsCredentialsProvider = new STSAssumeRoleSessionCredentialsProvider
                .Builder(config.getAwsRole(), "DataExchange")
                .withStsClient(sts)
                .build();

        ClientConfiguration clientConfiguration = new ClientConfiguration().withMaxErrorRetry(3);

        return AmazonS3ClientBuilder.standard()
                .withCredentials(awsCredentialsProvider).withRegion(config.getAwsRegion())
                .withClientConfiguration(clientConfiguration)
                .build();
    }
}
