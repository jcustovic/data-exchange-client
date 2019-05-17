package com.dataexchange.client.config.model;

import javax.validation.constraints.NotEmpty;

public class S3Configuration {

    @NotEmpty
    private String bucketName;
    @NotEmpty
    private String inputFolder;
    private String tarPattern;
    private Integer minItemsToZip;
    private Boolean serverSideEncryption = false;

    @NotEmpty
    private String awsRegion;
    @NotEmpty
    private String awsAccount;
    @NotEmpty
    private String awsAccessKey;
    @NotEmpty
    private String awsSecretKey;
    @NotEmpty
    private String awsRole;

    // Getters & setters

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    public String getTarPattern() {
        return tarPattern;
    }

    public void setTarPattern(String tarPattern) {
        this.tarPattern = tarPattern;
    }

    public Integer getMinItemsToZip() {
        return minItemsToZip;
    }

    public void setMinItemsToZip(Integer minItemsToZip) {
        this.minItemsToZip = minItemsToZip;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getAwsAccount() {
        return awsAccount;
    }

    public void setAwsAccount(String awsAccount) {
        this.awsAccount = awsAccount;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getAwsRole() {
        return awsRole;
    }

    public void setAwsRole(String awsRole) {
        this.awsRole = awsRole;
    }

    public Boolean getServerSideEncryption() {
        return serverSideEncryption;
    }

    public void setServerSideEncryption(Boolean serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
    }
}
