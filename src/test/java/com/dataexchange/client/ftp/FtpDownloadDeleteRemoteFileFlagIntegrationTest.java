package com.dataexchange.client.ftp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("download-ftp-remote-delete-false")
public class FtpDownloadDeleteRemoteFileFlagIntegrationTest extends FtpDownloadTestAbstract {

    @Test
    @Timeout(value = 20000, unit = TimeUnit.MILLISECONDS)
    public void whenInputRemoteFile_shouldBeDownloadedAndRemoteFileNotDeleted() throws InterruptedException {
        if (waitForFilesInFolder(outputFolder)) {
            assertThat(new File(outputFolder).listFiles()).isNotEmpty();
            assertThat(outputFile).exists();

            assertThat(new File(realRemoteFolder).listFiles()).isNotEmpty();
        }
    }
}
