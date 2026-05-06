package com.dataexchange.client.sftp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Arrays.isNullOrEmpty;
import static org.junit.jupiter.api.Assertions.fail;

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

    @Test
    @Timeout(value = 20000, unit = TimeUnit.MILLISECONDS)
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
