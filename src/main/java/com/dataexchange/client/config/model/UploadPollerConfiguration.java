package com.dataexchange.client.config.model;

import com.dataexchange.client.infrastructure.util.Conditional;

import javax.validation.constraints.NotEmpty;

@Conditional(selected = "fileType", exists = false, required = {"remoteOutputFolder"})
@Conditional(selected = "fileType", exists = true, required = {})
public class UploadPollerConfiguration {

    @NotEmpty
    private String name;
    @NotEmpty
    private String inputFolder;
    @NotEmpty
    private String processedFolder;
    private String remoteOutputFolder;
    private String regexFilter;
    private FileType fileType;
    private boolean useTempPrefix = false;

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

    public boolean isUseTempPrefix() {
        return useTempPrefix;
    }

    public void setUseTempPrefix(boolean useTempPrefix) {
        this.useTempPrefix = useTempPrefix;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
}
