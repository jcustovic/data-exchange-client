package com.dataexchange.client.sftp;

import org.assertj.core.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("download-sftp-s3")
public class SftpDownloadToS3IntegrationTest extends SftpDownloadToS3TestAbstract {

    @Test(timeout = 50_000)
    @Ignore
    public void whenInputRemoteImages_shouldBeDownloadedZippedAndUploadToS3() throws InterruptedException, IOException {
        File.createTempFile("image1", ".jpg", new File(realRemoteFolder));
        File.createTempFile("image2", ".jpg", new File(realRemoteFolder));
        File.createTempFile("image3", ".jpg", new File(realRemoteFolder));
        File.createTempFile("image4", ".jpg", new File(realRemoteFolder));
        TimeUnit.SECONDS.sleep(40);

        assertThat(Arrays.isNullOrEmpty(new File(realRemoteFolder).listFiles())).isTrue();
        assertThat(Arrays.isNullOrEmpty(new File(downloadPoller.getOutputFolder()).listFiles())).isTrue();
        assertThat(Arrays.isNullOrEmpty(new File(downloadPoller.getZipConfiguration().getOutputFolder()).listFiles())).isTrue();
    }
}
