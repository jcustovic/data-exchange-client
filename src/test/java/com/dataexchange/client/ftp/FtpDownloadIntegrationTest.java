package com.dataexchange.client.ftp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("download-ftp")
public class FtpDownloadIntegrationTest extends FtpDownloadTestAbstract {

    @Test
    @Timeout(value = 20000, unit = TimeUnit.MILLISECONDS)
    public void whenInputRemoteFile_shouldBeDownloadedAndMovedToOutputFolder() throws InterruptedException {
        if (waitForFilesInFolder(outputFolder)) {
            File outputFolderFile = new File(outputFolder);
            assertThat(new File(realRemoteFolder).listFiles()).isNullOrEmpty();
            assertThat(outputFolderFile.listFiles()).isNotEmpty();
            assertThat(outputFolderFile.listFiles()).hasSize(1);
            assertThat(outputFile).isEqualTo(outputFolderFile.listFiles()[0]);
        }
    }
}
