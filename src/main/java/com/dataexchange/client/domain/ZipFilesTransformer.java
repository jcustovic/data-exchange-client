package com.dataexchange.client.domain;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.time.format.DateTimeFormatter.ofPattern;

@Component
public class ZipFilesTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipFilesTransformer.class);

    @Transformer
    public Object transform(Object source, @Header("filename_pattern") String filenamePattern,
                            @Header("input_folder") String inputFolder) {

        List<File> files = (List<File>) source;
        String outputZipName = createZipFilename(inputFolder, filenamePattern);

        LOGGER.debug("Zipping file {}", outputZipName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputZipName);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            for (File file : files) {
                FileInputStream fis = new FileInputStream(file);
                ZipEntry zipEntry = new ZipEntry(file.getName());
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
            }
            zipOut.close();
            fos.close();

            LOGGER.info("File zipped {}", outputZipName);
            deleteSourceFiles(files);
        } catch (IOException e) {
            FileUtils.deleteQuietly(new File(outputZipName));
            LOGGER.error("Error on zipping files", e);
        }

        return fos == null ? null : new File(outputZipName);
    }

    private String createZipFilename(String inputFolder, String filenamePattern) {
        String zipFileName = filenamePattern.replace("%s", LocalDateTime.now().format(ofPattern("YYYY-MM-dd_HH-mm-ss")));
        return inputFolder + "/" + zipFileName;
    }

    private void deleteSourceFiles(List<File> files) {
        files.forEach(FileUtils::deleteQuietly);
    }
}
