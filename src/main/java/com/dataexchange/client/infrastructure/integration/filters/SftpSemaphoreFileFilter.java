package com.dataexchange.client.infrastructure.integration.filters;

import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.integration.file.filters.AbstractFileListFilter;

import java.util.HashSet;

public class SftpSemaphoreFileFilter extends AbstractFileListFilter<DirEntry> {

    private final Object monitor = new Object();

    private final String semaphoreSuffix;
    private HashSet<String> existingSemaphoreFiles = new HashSet<>();

    public SftpSemaphoreFileFilter(String semaphoreSuffix) {
        this.semaphoreSuffix = semaphoreSuffix;
    }

    @Override
    public boolean accept(DirEntry file) {
        synchronized (this.monitor) {
            if (file.getFilename().endsWith(semaphoreSuffix)) {
                existingSemaphoreFiles.add(file.getFilename());
                return true;
            } else {
                return existingSemaphoreFiles.contains(file.getFilename() + semaphoreSuffix);
            }
        }
    }
}
