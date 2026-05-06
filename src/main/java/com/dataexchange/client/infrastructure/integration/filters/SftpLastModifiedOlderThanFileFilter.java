package com.dataexchange.client.infrastructure.integration.filters;

import org.apache.sshd.sftp.client.SftpClient.Attributes;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.integration.file.filters.AbstractFileListFilter;

import java.time.Instant;

public class SftpLastModifiedOlderThanFileFilter extends AbstractFileListFilter<DirEntry> {

    private final int minutes;

    public SftpLastModifiedOlderThanFileFilter(int minutes) {
        this.minutes = minutes;
    }

    @Override
    public boolean accept(DirEntry file) {
        Attributes attrs = file.getAttributes();
        Instant modifyTime = attrs.getModifyTime().toInstant();
        if (modifyTime != null) {
            Instant threshold = Instant.now().minusSeconds(minutes * 60L);
            return modifyTime.isBefore(threshold);
        }
        return true;
    }
}
