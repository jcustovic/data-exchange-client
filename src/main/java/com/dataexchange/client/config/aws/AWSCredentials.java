package com.dataexchange.client.config.aws;

import com.amazonaws.regions.Regions;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties("aws.credentials")
public class AWSCredentials {

    private Regions region;
    private String accessKey;
    private String secretKey;
    private String stsRoleArn;
    private String roleSessionName;

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getStsRoleArn() {
        return stsRoleArn;
    }

    public void setStsRoleArn(String stsRoleArn) {
        this.stsRoleArn = stsRoleArn;
    }

    public String getRoleSessionName() {
        return roleSessionName;
    }

    public void setRoleSessionName(String roleSessionName) {
        this.roleSessionName = roleSessionName;
    }
}
