package com.dataexchange.client.sftp;

import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.assertj.core.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

@SpringBootTest
@DirtiesContext
@RunWith(SpringRunner.class)
public abstract class SftpTestServer {

    private static final String SFTP_SERVER_FOLDER = "sftp-server";
    private static final int PORT = 8888;
    
    private SshServer sshd;
    File sftpRootFolder;

    @Before
    public void setup() throws Exception {
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(PORT);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.setPasswordAuthenticator((username, password, session) -> true);
        //sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        Path sftpRootFolderPath = Files.createTempDirectory(SFTP_SERVER_FOLDER);
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(sftpRootFolderPath));
        sftpRootFolder = sftpRootFolderPath.toFile();
        sshd.start();
    }

    @After
    public void teardown() throws Exception {
        sshd.stop();
        while (!sshd.isClosed()) {
            Thread.sleep(100);
        }
        Thread.sleep(50);
        FileUtils.deleteQuietly(sftpRootFolder);
    }

    public static boolean waitForFilesInFolder(String folder) throws InterruptedException {
        while (true) {
            if (!Arrays.isNullOrEmpty(new File(folder).listFiles())) {
                return true;
            }
            Thread.sleep(100);
        }
    }
}
