package com.dataexchange.client.sftp;

import com.dataexchange.client.config.model.DownloadPollerConfiguration;
import com.dataexchange.client.config.model.MainConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

public abstract class SftpDownloadToS3TestAbstract extends SftpTestServer {

    @Autowired
    MainConfiguration config;

    String realRemoteFolder;
    String outputFolder;
    DownloadPollerConfiguration downloadPoller;

    @BeforeEach
    public void setup() throws Exception {
        super.setup();
        downloadPoller = config.getSftps().get(0).getDownloadPollers().get(0);
        outputFolder = downloadPoller.getOutputFolder();
        realRemoteFolder = sftpRootFolder.toString() + File.separator + downloadPoller.getRemoteInputFolder();

        FileUtils.forceMkdir(new File(realRemoteFolder));
        FileUtils.forceMkdir(new File(outputFolder));

//        cleanupWorkingDirs();
    }

    @AfterEach
    public void teardown() throws Exception {
        super.teardown();
//        cleanupWorkingDirs();
    }

    private void cleanupWorkingDirs() throws IOException {
        FileUtils.cleanDirectory(new File(outputFolder));
    }
}
