package com.dataexchange.client.infrastructure.integration.filters;

import com.jcraft.jsch.ChannelSftp;
import org.springframework.integration.file.filters.AbstractFileListFilter;

import java.util.Date;

public class SftpLastModifiedOlderThanFileFilter extends AbstractFileListFilter<ChannelSftp.LsEntry> {

    private final int minutes;

    public SftpLastModifiedOlderThanFileFilter(int minutes) {
        this.minutes = minutes;
    }

    @Override
    public boolean accept(ChannelSftp.LsEntry file) {
        int modifiedTimeInSec = file.getAttrs().getMTime();
        if (modifiedTimeInSec > 0) {
            long currentSeconds = new Date().getTime() / 1000;

            return (currentSeconds > modifiedTimeInSec + (minutes * 60));
        }

        return true;
    }
}
