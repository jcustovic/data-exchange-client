package com.dataexchange.client.config.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.paramstore.AwsParamStoreBootstrapConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;


@Configuration
@EnableConfigurationProperties(AWSCredentials.class)
@AutoConfigureBefore(value = AwsParamStoreBootstrapConfiguration.class)
@ConditionalOnProperty(
        prefix = "aws.credentials",
        name = {"use-static-provider"}
)
public class AWSConfiguration {

    @Bean
    public AWSSimpleSystemsManagement ssmClient(AWSCredentials credentials) {
        return AWSSimpleSystemsManagementClient.builder()
                .withCredentials(buildCredentialsProvider(credentials))
                .withRegion(credentials.getRegion())
                .build();
    }

    private AWSCredentialsProvider buildCredentialsProvider(AWSCredentials credentials) {
        if (isNullOrEmpty(credentials.getStsRoleArn())) {
            return buildDefaultCredentialsProvider(credentials);
        } else {
            return new STSAssumeRoleSessionCredentialsProvider.Builder(credentials.getStsRoleArn(), credentials.getRoleSessionName())
                    .withStsClient(buildSecurityTokenService(credentials))
                    .build();
        }
    }

    private AWSSecurityTokenService buildSecurityTokenService(AWSCredentials credentials) {
        return AWSSecurityTokenServiceClientBuilder.standard()
                .withCredentials(buildDefaultCredentialsProvider(credentials))
                .withRegion(credentials.getRegion())
                .build();
    }

    private AWSCredentialsProvider buildDefaultCredentialsProvider(AWSCredentials credentials) {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(credentials.getAccessKey(), credentials.getSecretKey()));
    }

}
