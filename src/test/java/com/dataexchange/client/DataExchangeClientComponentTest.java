package com.dataexchange.client;

import com.dataexchange.client.domain.ConnectionMonitor;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static com.dataexchange.client.sftp.SftpTestServer.waitForFilesInFolder;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("upload-sftp")
public class DataExchangeClientComponentTest {

    @Autowired
    private MessageChannel moveFileChannel;

    private String inputFolder;
    private String processedFolder;

    private File sourceFile;
    private File processedFile;
    private Path tempDir;

    @MockBean
    private ConnectionMonitor connectionMonitor;

    @Before
    public void setup() throws Exception {
        tempDir = Files.createTempDirectory("data_exchange_test");
        inputFolder = tempDir.toAbsolutePath().toString() + File.separator + "input";
        processedFolder = tempDir.toAbsolutePath().toString() + File.separator + "processed";

        FileUtils.forceMkdir(new File(inputFolder));
        FileUtils.forceMkdir(new File(processedFolder));

        sourceFile = File.createTempFile("anyfile", ".txt", new File(inputFolder));
    }

    @After
    public void teardown() throws Exception {
        processedFile.delete();
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    @Test(timeout = 10000)
    public void whenFileSentToMoveFileChannel_itIsMovedToProcessedFolder() throws InterruptedException {
        Message<File> message = MessageBuilder.withPayload(sourceFile).build();

        HeaderEnricher headerEnricher = new HeaderEnricher(Collections.singletonMap("destination_folder", new
                StaticHeaderValueMessageProcessor<>(processedFolder)));
        Message<?> messageEnriched = headerEnricher.transform(message);

        AdviceMessage adviceMessage = new AdviceMessage<>(messageEnriched.getPayload(), messageEnriched);

        this.moveFileChannel.send(adviceMessage);

        processedFile = new File(processedFolder + File.separator + sourceFile.getName());

        if (waitForFilesInFolder(processedFolder)) {
            assertThat(processedFile.exists()).isTrue();
            assertThat(Arrays.isNullOrEmpty(new File(inputFolder).listFiles())).isTrue();
        }
    }
}
