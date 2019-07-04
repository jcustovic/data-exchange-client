package com.dataexchange.client.config.model;

public class FtpPollerConfiguration extends BasePollerConfiguration {

    private String ftpParserDateFormat;

    @Override
    public Integer getPort() {
        return super.getPort() == null ? 21 : super.getPort();
    }

    // Getters & setters

    public String getFtpParserDateFormat() {
        return ftpParserDateFormat;
    }

    public void setFtpParserDateFormat(String ftpParserDateFormat) {
        this.ftpParserDateFormat = ftpParserDateFormat;
    }
}
