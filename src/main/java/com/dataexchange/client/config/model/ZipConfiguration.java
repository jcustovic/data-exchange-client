package com.dataexchange.client.config.model;

import javax.validation.constraints.NotEmpty;

public class ZipConfiguration {

    @NotEmpty
    private String outputFolder;
    @NotEmpty
    private String pattern;
    @NotEmpty
    private Integer minItemsToZip;

    // Getters & setters

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public Integer getMinItemsToZip() {
        return minItemsToZip;
    }

    public void setMinItemsToZip(Integer minItemsToZip) {
        this.minItemsToZip = minItemsToZip;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
