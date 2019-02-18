package com.dataexchange.client.config.model;

import javax.validation.constraints.NotEmpty;

public class DownloadPollerConfiguration {

    @NotEmpty
    private String name;
    @NotEmpty
    private String downloadFolder;
    @NotEmpty
    private String outputFolder;
    private String outputFileNameExpression;
    @NotEmpty
    private String remoteInputFolder;
    private String regexFilter;
    private boolean deleteRemoteFile = true;
    private String semaphoreFileSuffix;
    private Long pollIntervalMilliseconds = 60000L;
    private String pollCron;
    private Integer modifiedDateAfterMinutes;

    private S3Configuration s3Configuration;
    private ZipConfiguration zipConfiguration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDownloadFolder() {
        return downloadFolder;
    }

    public void setDownloadFolder(String downloadFolder) {
        this.downloadFolder = downloadFolder;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public String getOutputFileNameExpression() {
        return outputFileNameExpression;
    }

    public void setOutputFileNameExpression(String outputFileNameExpression) {
        this.outputFileNameExpression = outputFileNameExpression;
    }

    public String getRemoteInputFolder() {
        return remoteInputFolder;
    }

    public void setRemoteInputFolder(String remoteInputFolder) {
        this.remoteInputFolder = remoteInputFolder;
    }

    public String getRegexFilter() {
        return regexFilter;
    }

    public void setRegexFilter(String regexFilter) {
        this.regexFilter = regexFilter;
    }

    public boolean isDeleteRemoteFile() {
        return deleteRemoteFile;
    }

    public void setDeleteRemoteFile(boolean deleteRemoteFile) {
        this.deleteRemoteFile = deleteRemoteFile;
    }

    public String getSemaphoreFileSuffix() {
        return semaphoreFileSuffix;
    }

    public void setSemaphoreFileSuffix(String semaphoreFileSuffix) {
        this.semaphoreFileSuffix = semaphoreFileSuffix;
    }

    public Long getPollIntervalMilliseconds() {
        return pollIntervalMilliseconds;
    }

    public void setPollIntervalMilliseconds(Long pollIntervalMilliseconds) {
        this.pollIntervalMilliseconds = pollIntervalMilliseconds;
    }

    public String getPollCron() {
        return pollCron;
    }

    public void setPollCron(String pollCron) {
        this.pollCron = pollCron;
    }

    public Integer getModifiedDateAfterMinutes() {
        return modifiedDateAfterMinutes;
    }

    public void setModifiedDateAfterMinutes(Integer modifiedDateAfterMinutes) {
        this.modifiedDateAfterMinutes = modifiedDateAfterMinutes;
    }

    public S3Configuration getS3Configuration() {
        return s3Configuration;
    }

    public void setS3Configuration(S3Configuration s3Configuration) {
        this.s3Configuration = s3Configuration;
    }

    public ZipConfiguration getZipConfiguration() {
        return zipConfiguration;
    }

    public void setZipConfiguration(ZipConfiguration zipConfiguration) {
        this.zipConfiguration = zipConfiguration;
    }
}
