package com.dataexchange.client.infrastructure.integration.filters;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.file.filters.AbstractFileListFilter;

import java.util.HashSet;

public class FtpSemaphoreFileFilter extends AbstractFileListFilter<FTPFile> {

    private final Object monitor = new Object();

    private final String semaphoreSuffix;
    private HashSet<String> existingSemaphoreFiles = new HashSet<>();

    public FtpSemaphoreFileFilter(String semaphoreSuffix) {
        this.semaphoreSuffix = semaphoreSuffix;
    }

    @Override
    public boolean accept(FTPFile file) {
        synchronized (this.monitor) {
            if (file.getName().endsWith(semaphoreSuffix)) {
                existingSemaphoreFiles.add(file.getName());
                return true;
            } else {
                return existingSemaphoreFiles.contains(file.getName() + semaphoreSuffix);
            }
        }
    }
}
