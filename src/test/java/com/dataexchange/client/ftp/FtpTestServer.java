package com.dataexchange.client.ftp;

import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.util.Arrays.isNullOrEmpty;

@SpringBootTest
@DirtiesContext
@RunWith(SpringRunner.class)
public abstract class FtpTestServer {

    static final Logger LOGGER = LoggerFactory.getLogger(FtpTestServer.class);

    private static final String FTP_SERVER_FOLDER = "ftp-server";
    private static final int PORT = 12345;
    File ftpRootFolder;
    private FtpServer server;

    static boolean waitForFilesInFolder(String folder) throws InterruptedException {
        while (true) {
            if (!isNullOrEmpty(new File(folder).listFiles())) {
                return true;
            }
            Thread.sleep(100);
        }
    }

    public void setup() throws Exception {
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor());
        UserManager userManager = userManagerFactory.createUserManager();
        BaseUser user = new BaseUser();
        user.setName("test");
        user.setPassword("test");
        user.setEnabled(true);
        Path ftpRootFolderPath = Files.createTempDirectory(FTP_SERVER_FOLDER);
        user.setHomeDirectory(ftpRootFolderPath.toAbsolutePath().toString());
        user.setAuthorities(Arrays.asList(new Authority[]{new WritePermission()}));
        userManager.save(user);

        FtpServerFactory serverFactory = new FtpServerFactory();
        serverFactory.setUserManager(userManager);
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(PORT);
        serverFactory.addListener("default", factory.createListener());

        server = serverFactory.createServer();
        server.start();

        ftpRootFolder = ftpRootFolderPath.toFile();
    }

    public void teardown() throws Exception {
        LOGGER.info("Stopping FTP server...");
        server.stop();
        while (!server.isStopped()) {
            Thread.sleep(100);
        }
        Thread.sleep(50);
        LOGGER.info("FTP server is stopped.");

        FileUtils.deleteQuietly(ftpRootFolder);
    }
}
