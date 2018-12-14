package com.dataexchange.client.sftp;

import com.dataexchange.client.config.MainConfiguration;
import com.dataexchange.client.config.UploadPollerConfiguration;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("upload-sftp")
public class SftpUploadIntegrationTest extends SftpTestServer {

    @Autowired
    private MainConfiguration mainConfiguration;

    private String remoteOutputFolder;
    private String inputFolder;
    private String processedFolder;
    private File sourceFile;

    @Before
    public void setup() throws Exception {
        super.setup();
        UploadPollerConfiguration uploadPoller = mainConfiguration.getSftps().get(0).getUploadPollers().get(0);
        inputFolder = uploadPoller.getInputFolder();
        processedFolder = uploadPoller.getProcessedFolder();

        FileUtils.forceMkdir(new File(inputFolder));
        FileUtils.forceMkdir(new File(processedFolder));

        remoteOutputFolder = uploadPoller.getRemoteOutputFolder();
        sourceFile = File.createTempFile("anyfile", ".txt", new File(inputFolder));
    }

    @After
    public void teardown() throws Exception {
        super.teardown();
        FileUtils.cleanDirectory(new File(inputFolder));
        FileUtils.cleanDirectory(new File(processedFolder));
    }

    @Test(timeout = 20000)
    public void whenInputFile_shouldBeUploadedAndMovedToProcessedFolder() throws InterruptedException {
        if (waitForFilesInFolder(this.processedFolder)) {
            assertThat(Arrays.isNullOrEmpty(new File(inputFolder).listFiles())).isTrue();
            assertThat(Arrays.isNullOrEmpty(new File(processedFolder).listFiles())).isFalse();
            assertThat(new File(processedFolder + File.separator + sourceFile.getName()).exists()).isTrue();

            String realRemoteFolder = sftpRootFolder.toString() + File.separator + remoteOutputFolder;
            assertThat(Arrays.isNullOrEmpty(new File(realRemoteFolder).listFiles())).isFalse();
            assertThat(new File(realRemoteFolder + File.separator + sourceFile.getName()).exists()).isTrue();
        }
    }
}