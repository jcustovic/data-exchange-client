package com.dataexchange.client.ftp;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("download-ftp-remote-delete-false")
public class FtpDownloadDeleteRemoteFileFlagIntegrationTest extends FtpDownloadTestAbstract {

    @Test(timeout = 20000)
    public void whenInputRemoteFile_shouldBeDownloadedAndRemoteFileNotDeleted() throws InterruptedException, SftpException, JSchException {
        if (waitForFilesInFolder(outputFolder)) {
            assertThat(new File(outputFolder).listFiles()).isNotEmpty();
            assertThat(outputFile).exists();

            assertThat(new File(realRemoteFolder).listFiles()).isNotEmpty();
        }
    }
}
