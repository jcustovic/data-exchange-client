package com.dataexchange.client.sftp;

import com.dataexchange.client.config.model.DownloadPollerConfiguration;
import com.dataexchange.client.config.model.MainConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

public abstract class SftpDownloadToS3TestAbstract extends SftpTestServer {

    @Autowired
    MainConfiguration config;

    String realRemoteFolder;
    String outputFolder;
    DownloadPollerConfiguration downloadPoller;

    @Before
    public void setup() throws Exception {
        super.setup();
        downloadPoller = config.getSftps().get(0).getDownloadPollers().get(0);
        outputFolder = downloadPoller.getOutputFolder();
        realRemoteFolder = sftpRootFolder.toString() + File.separator + downloadPoller.getRemoteInputFolder();

        FileUtils.forceMkdir(new File(realRemoteFolder));
        FileUtils.forceMkdir(new File(outputFolder));

//        cleanupWorkingDirs();
    }

    @After
    public void teardown() throws Exception {
        super.teardown();
        cleanupWorkingDirs();
    }

    private void cleanupWorkingDirs() throws IOException {
        FileUtils.cleanDirectory(new File(outputFolder));
    }
}
