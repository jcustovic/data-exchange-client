package com.dataexchange.client.config.flows;

import com.dataexchange.client.config.DynamicConfigurationCreator;
import com.dataexchange.client.config.model.S3Configuration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.core.GenericHandler;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.io.File;

import static com.dataexchange.client.domain.util.LogHelper.clearLogContext;
import static com.dataexchange.client.domain.util.LogHelper.encrichLogsWithConnectionInfo;

@Component
public class S3Flow {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigurationCreator.class);

    @Autowired
    private IntegrationFlowContext integrationFlowContext;

    public void uploadSetup(String configName, S3Configuration config, String username) {
        IntegrationFlow s3Flow = IntegrationFlow
                .from(Files.inboundAdapter(new File(config.getInputFolder())).preventDuplicates(true).autoCreateDirectory(true),
                        conf -> conf.poller(Pollers.fixedRate(2000).maxMessagesPerPoll(100)))
                .enrichHeaders(h -> h.headerExpression(FileHeaders.ORIGINAL_FILE, "payload.path"))
                .handle((GenericHandler<File>) (payload, headers) -> {
                    String filename = (String) headers.get(FileHeaders.FILENAME);
                    String bucketName = resolveBucketName(config.getBucketName(), filename);

                    S3Client s3Client = initS3Client(config);

                    PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(filename);

                    if (config.getServerSideEncryption()) {
                        requestBuilder.serverSideEncryption(ServerSideEncryption.AES256);
                    }

                    try {
                        s3Client.putObject(requestBuilder.build(), RequestBody.fromFile(payload));
                        return true;
                    } catch (Exception e) {
                        LOGGER.error("Upload to S3 has failed for file {}", filename, e);
                        return false;
                    }
                }, conf -> conf.advice(encrichLogsWithConnectionInfo(username, config.getInputFolder()), clearLogContext()))
                .<Boolean, Boolean>route(success -> success, mapping -> mapping
                        .subFlowMapping(true, sf -> sf.handle(message -> {
                                    String originalFile = (String) message.getHeaders().get(FileHeaders.ORIGINAL_FILE);
                                    LOGGER.info("Deleting file: {}", originalFile);
                                    FileUtils.deleteQuietly(new File(originalFile));
                                }, conf -> conf.advice(encrichLogsWithConnectionInfo(username, config.getInputFolder())))
                        )
                        .subFlowMapping(false, sf -> sf.delay(c -> c.id("s3-error-delayer")
                                .defaultDelay(600_000)).channel("s3UploadFlow-channel")
                        ))
                .get();

        String beanName = "s3UploadFlow-" + configName;
        integrationFlowContext.registration(s3Flow).id(beanName).autoStartup(true).register();
    }

    private String resolveBucketName(String bucketName, String filename) {
        if (filename.endsWith(".zip") || filename.endsWith(".tar")) {
            return bucketName + "/zip";
        }

        return bucketName + "/jpg/" + filename.substring(0, 6);
    }

    private S3Client initS3Client(S3Configuration config) {
        StaticCredentialsProvider basicCredentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.getAwsAccessKey(), config.getAwsSecretKey()));

        Region region = Region.of(config.getAwsRegion());

        StsClient stsClient = StsClient.builder()
                .credentialsProvider(basicCredentials)
                .region(region)
                .build();

        StsAssumeRoleCredentialsProvider assumeRoleProvider = StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(AssumeRoleRequest.builder()
                        .roleArn(config.getAwsRole())
                        .roleSessionName("DataExchange")
                        .build())
                .build();

        return S3Client.builder()
                .credentialsProvider(assumeRoleProvider)
                .region(region)
                .build();
    }
}
