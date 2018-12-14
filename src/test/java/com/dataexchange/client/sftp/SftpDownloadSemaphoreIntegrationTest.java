package com.dataexchange.client.sftp;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Arrays.isNullOrEmpty;
import static org.junit.Assert.fail;

@ActiveProfiles("download-sftp-semaphore")
public class SftpDownloadSemaphoreIntegrationTest extends SftpDownloadTestAbstract {

    @Test
    public void whenRemoteFileWithoutSemaphore_shouldNOTBeDownloadedAndRemoteFileWillStayThere() throws InterruptedException {
        int i = 0;
        while (i++ < 20) {
            if (!isNullOrEmpty(new File(outputFolder).listFiles())) {
                fail("Should not reach this point. The file must not be downloaded");
            }
            Thread.sleep(1000);
        }
        assertThat(new File(realRemoteFolder).listFiles()).hasSize(1);
        assertThat(outputFile).doesNotExist();
    }

    @Test(timeout = 20000)
    public void whenRemoteFileWithSemaphore_shouldBeDownloadedAndRemoteFileWillStayThere() throws InterruptedException,
            IOException {
        // Create semaphore file
        File semFile = new File(realRemoteFolder, remoteSourceFile.getName() + downloadPoller.getSemaphoreFileSuffix());
        semFile.createNewFile();

        if (waitForFilesInFolder(outputFolder)) {
            assertThat(new File(outputFolder).listFiles()).hasSize(1);
            assertThat(outputFile).exists();

            assertThat(new File(realRemoteFolder).listFiles()).isEmpty();
        }
    }
}
