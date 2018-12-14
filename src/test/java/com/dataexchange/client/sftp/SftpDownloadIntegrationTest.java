package com.dataexchange.client.sftp;

import org.assertj.core.util.Arrays;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("download-sftp")
public class SftpDownloadIntegrationTest extends SftpDownloadTestAbstract {

    @Test(timeout = 20000)
    public void whenInputRemoteFile_shouldBeDownloadedAndMovedToOutputFolder() throws InterruptedException {
        if (waitForFilesInFolder(outputFolder)) {
            assertThat(Arrays.isNullOrEmpty(new File(realRemoteFolder).listFiles())).isTrue();
            assertThat(Arrays.isNullOrEmpty(new File(outputFolder).listFiles())).isFalse();
            assertThat(outputFile.exists()).isTrue();
        }
    }
}
