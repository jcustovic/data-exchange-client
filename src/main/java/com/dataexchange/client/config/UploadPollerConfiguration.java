package com.dataexchange.client.config;

import org.hibernate.validator.constraints.NotEmpty;

public class UploadPollerConfiguration {

    @NotEmpty
    private String name;
    @NotEmpty
    private String inputFolder;
    @NotEmpty
    private String processedFolder;
    @NotEmpty
    private String remoteOutputFolder;
    private String regexFilter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    public String getProcessedFolder() {
        return processedFolder;
    }

    public void setProcessedFolder(String processedFolder) {
        this.processedFolder = processedFolder;
    }

    public String getRemoteOutputFolder() {
        return remoteOutputFolder;
    }

    public void setRemoteOutputFolder(String remoteOutputFolder) {
        this.remoteOutputFolder = remoteOutputFolder;
    }

    public String getRegexFilter() {
        return regexFilter;
    }

    public void setRegexFilter(String regexFilter) {
        this.regexFilter = regexFilter;
    }
}
