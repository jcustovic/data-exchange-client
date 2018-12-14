package com.dataexchange.client.ftp;

import com.dataexchange.client.config.MainConfiguration;
import com.dataexchange.client.config.UploadPollerConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("upload-ftp")
@DirtiesContext
public class FtpUploadIntegrationTest extends FtpTestServer {

    @Autowired
    private MainConfiguration config;

    private String inputFolder;
    private String processedFolder;
    private String remoteOutputFolder;
    private File sourceFile;

    @After
    public void teardown() throws Exception {
        super.teardown();
        FileUtils.cleanDirectory(new File(inputFolder));
        FileUtils.cleanDirectory(new File(processedFolder));
    }

    @Before
    public void setup() throws Exception {
        super.setup();
        UploadPollerConfiguration uploadPoller = config.getFtps().get(0).getUploadPollers().get(0);
        inputFolder = uploadPoller.getInputFolder();
        processedFolder = uploadPoller.getProcessedFolder();

        FileUtils.forceMkdir(new File(inputFolder));
        FileUtils.forceMkdir(new File(processedFolder));

        remoteOutputFolder = uploadPoller.getRemoteOutputFolder();
        sourceFile = File.createTempFile("anyfile", ".txt", new File(inputFolder));
    }

    @Test(timeout = 20000)
    public void whenInputFile_shouldBeUploadedAndMovedToProcessedFolder() throws InterruptedException {
        if (waitForFilesInFolder(this.processedFolder)) {
            assertThat(new File(inputFolder).listFiles()).isNullOrEmpty();
            assertThat(new File(processedFolder).listFiles()).isNotEmpty();
            assertThat(new File(processedFolder + File.separator + sourceFile.getName())).exists();

            String realRemoteFolder = ftpRootFolder.toString() + File.separator + remoteOutputFolder;
            assertThat(new File(realRemoteFolder).listFiles()).isNotEmpty();
            assertThat(new File(realRemoteFolder + File.separator + sourceFile.getName())).exists();
        }
    }
}