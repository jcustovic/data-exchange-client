package com.dataexchange.client.infrastructure.integration.filters;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.file.filters.AbstractFileListFilter;

import java.util.Date;

public class FtpLastModifiedOlderThanFileFilter extends AbstractFileListFilter<FTPFile> {

    private final int minutes;

    public FtpLastModifiedOlderThanFileFilter(int minutes) {
        this.minutes = minutes;
    }

    @Override
    protected boolean accept(FTPFile file) {
        long modifiedTimeInSec = file.getTimestamp().getTimeInMillis();
        if (modifiedTimeInSec > 0) {
            long currentSeconds = new Date().getTime();

            return (currentSeconds > modifiedTimeInSec + (minutes * 60 * 1000));
        }

        return true;
    }
}
