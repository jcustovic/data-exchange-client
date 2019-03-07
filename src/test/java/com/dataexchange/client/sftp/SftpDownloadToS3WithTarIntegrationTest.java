package com.dataexchange.client.sftp;

import org.assertj.core.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("download-sftp-s3-tar")
public class SftpDownloadToS3WithTarIntegrationTest extends SftpDownloadToS3TestAbstract {

    @Test(timeout = 50_000)
    @Ignore
    public void whenInputRemoteTarFile_shouldBeDownloadedAndPushedToS3() throws InterruptedException, IOException {
        File.createTempFile("image666", ".tar", new File(realRemoteFolder));
        TimeUnit.SECONDS.sleep(40);

        assertThat(Arrays.isNullOrEmpty(new File(realRemoteFolder).listFiles())).isTrue();
        assertThat(Arrays.isNullOrEmpty(new File(downloadPoller.getS3Configuration().getInputFolder()).listFiles())).isTrue();
    }
}
