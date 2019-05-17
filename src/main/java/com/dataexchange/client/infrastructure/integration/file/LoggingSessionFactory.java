package com.dataexchange.client.infrastructure.integration.file;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public class LoggingSessionFactory<T> extends CachingSessionFactory<T> {

    private enum OperationType {
        DOWNLOAD, UPLOAD
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSessionFactory.class);

    private final RestHighLevelClient esClient;
    private final String indexPatternSpel;
    private final String connectionUsername;
    private final String connectionHost;
    private final SpelExpressionParser spelExpressionParser;
    private final Executor submitExecutor;

    public LoggingSessionFactory(RestHighLevelClient esClient, String indexPatternSpel, SessionFactory<T> sessionFactory,
                                 int sessionCacheSize, String connectionUsername, String connectionHost) {
        super(sessionFactory, sessionCacheSize);

        Assert.notNull(esClient, "RestHighLevelClient must not be null");
        Assert.notNull(indexPatternSpel, "IndexPatternSpel must not be null");

        this.esClient = esClient;
        this.indexPatternSpel = indexPatternSpel;
        this.connectionUsername = connectionUsername;
        this.connectionHost = connectionHost;
        spelExpressionParser = new SpelExpressionParser();
        submitExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public Session<T> getSession() {
        return new LoggingSession(super.getSession());
    }

    public class LoggingSession implements Session<T> {

        private final Session<T> targetSession;

        private LoggingSession(Session<T> targetSession) {
            this.targetSession = targetSession;
        }

        @Override
        public boolean remove(String path) throws IOException {
            return targetSession.remove(path);
        }

        @Override
        public T[] list(String path) throws IOException {
            return targetSession.list(path);
        }

        @Override
        public void read(String source, OutputStream outputStream) throws IOException {
            CountingOutputStream fos = new CountingOutputStream(outputStream);
            long startTime = System.nanoTime();
            LOGGER.info("Reading started {}", source);
            targetSession.read(source, fos);
            long endTime = System.nanoTime();
            BigDecimal bytesPerSec = new BigDecimal(fos.getByteCount() / ((endTime - startTime) / 1000000000f))
                    .setScale(4, BigDecimal.ROUND_HALF_UP);
            BigDecimal kBytesPerSec = bytesPerSec.divide(BigDecimal.valueOf(1024f), 2, RoundingMode.HALF_UP);

            submitExecutor.execute(() -> {
                LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM"));
                IndexRequest request = createIndexRequest(source, fos.getCount(), kBytesPerSec, OperationType.DOWNLOAD);
                try {
                    esClient.index(request, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    LOGGER.error("Push statistics to ES failed", e);
                }
            });
        }

        @Override
        public void write(InputStream inputStream, String destination) throws IOException {
            CountingInputStream cis = new CountingInputStream(inputStream);
            long startTime = System.nanoTime();
            targetSession.write(cis, destination);
            long endTime = System.nanoTime();
            BigDecimal bytesPerSec = new BigDecimal(cis.getByteCount() / ((endTime - startTime) / 1000000000f))
                    .setScale(4, BigDecimal.ROUND_HALF_UP);
            BigDecimal kBytesPerSec = bytesPerSec.divide(BigDecimal.valueOf(1024f), 2, RoundingMode.HALF_UP);

            submitExecutor.execute(() -> {
                IndexRequest request = createIndexRequest(destination, cis.getCount(), kBytesPerSec, OperationType.UPLOAD);
                try {
                    esClient.index(request, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    LOGGER.error("Push statistics to ES failed", e);
                }
            });
        }

        @Override
        public void append(InputStream inputStream, String destination) throws IOException {
            targetSession.append(inputStream, destination);
        }

        @Override
        public boolean mkdir(String directory) throws IOException {
            return targetSession.mkdir(directory);
        }

        @Override
        public boolean rmdir(String directory) throws IOException {
            return targetSession.rmdir(directory);
        }

        @Override
        public void rename(String pathFrom, String pathTo) throws IOException {
            targetSession.rename(pathFrom, pathTo);
        }

        @Override
        public void close() {
            targetSession.close();
        }

        @Override
        public boolean isOpen() {
            return targetSession.isOpen();
        }

        @Override
        public boolean exists(String path) throws IOException {
            return targetSession.exists(path);
        }

        @Override
        public String[] listNames(String path) throws IOException {
            return targetSession.listNames(path);
        }

        @Override
        public InputStream readRaw(String source) throws IOException {
            return targetSession.readRaw(source);
        }

        @Override
        public boolean finalizeRaw() throws IOException {
            return targetSession.finalizeRaw();
        }

        @Override
        public Object getClientInstance() {
            return targetSession.getClientInstance();
        }

        @Override
        public boolean test() {
            return targetSession.test();
        }

        @Override
        public void dirty() {
            targetSession.dirty();
        }
    }

    private IndexRequest createIndexRequest(String filePath, long fileSize, BigDecimal kBytesPerSec, OperationType operationType) {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("@timestamp", ZonedDateTime.now().format(ISO_OFFSET_DATE_TIME));
        jsonMap.put("fileName", filePath);
        jsonMap.put("fileSizeBytes", fileSize);
        jsonMap.put("operationType", operationType);
        jsonMap.put("throughputKBs", kBytesPerSec.doubleValue());
        jsonMap.put("username", connectionUsername);
        jsonMap.put("host", connectionHost);

        return new IndexRequest().index(spelExpressionParser.parseExpression(indexPatternSpel).getValue(String.class))
                .type("default")
                .source(jsonMap);
    }
}
