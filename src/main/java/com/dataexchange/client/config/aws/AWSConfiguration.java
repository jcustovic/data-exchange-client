package com.dataexchange.client.config.aws;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import static org.springframework.util.StringUtils.hasText;


@Configuration
@EnableConfigurationProperties(AWSCredentials.class)
@ConditionalOnProperty(
        prefix = "aws.credentials",
        name = {"use-static-provider"}
)
public class AWSConfiguration {

    @Bean
    public SsmClient ssmClient(AWSCredentials credentials) {
        return SsmClient.builder()
                .credentialsProvider(buildCredentialsProvider(credentials))
                .region(credentials.getRegion())
                .build();
    }

    private AwsCredentialsProvider buildCredentialsProvider(AWSCredentials credentials) {
        if (!hasText(credentials.getStsRoleArn())) {
            return buildDefaultCredentialsProvider(credentials);
        } else {
            StsClient stsClient = StsClient.builder()
                    .credentialsProvider(buildDefaultCredentialsProvider(credentials))
                    .region(credentials.getRegion())
                    .build();
            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(AssumeRoleRequest.builder()
                            .roleArn(credentials.getStsRoleArn())
                            .roleSessionName(credentials.getRoleSessionName())
                            .build())
                    .build();
        }
    }

    private StaticCredentialsProvider buildDefaultCredentialsProvider(AWSCredentials credentials) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(credentials.getAccessKey(), credentials.getSecretKey()));
    }
}
