package com.dataexchange.client.ftp;

import com.dataexchange.client.config.DownloadPollerConfiguration;
import com.dataexchange.client.config.MainConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

public abstract class FtpDownloadTestAbstract extends FtpTestServer {

    @Autowired
    MainConfiguration config;

    String realRemoteFolder;
    File remoteSourceFile;
    String outputFolder;
    File outputFile;
    DownloadPollerConfiguration downloadPoller;

    @Before
    public void setup() throws Exception {
        super.setup();
        downloadPoller = config.getFtps().get(0).getDownloadPollers().get(0);
        outputFolder = downloadPoller.getOutputFolder();
        realRemoteFolder = ftpRootFolder.toString() + File.separator + downloadPoller.getRemoteInputFolder();
        LOGGER.info("Creating new remote folder {}", realRemoteFolder);

        FileUtils.forceMkdir(new File(outputFolder));
        FileUtils.forceMkdir(new File(realRemoteFolder));

        cleanupWorkingDirs();

        remoteSourceFile = File.createTempFile("anyfile", ".txt", new File(realRemoteFolder));
        outputFile = new File(outputFolder + File.separator + remoteSourceFile.getName());
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
