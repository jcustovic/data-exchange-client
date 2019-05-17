The project **Data Exchange Client** allows you to connect to SFTP or FTP servers and configure upload and download pullers.

Current stable version: 1.3.0

## How to use

The setup is quite easy and it requires you to only edit application.yaml configuration file.

To directly start the app run:
```maven
mvn spring-boot:run
```

To produce executable jar and run:

```maven
mvn clean install
java -jar target/data-exchange-client*.jar
```

### How to configure your app

The configuration is split up in 2 parts:
* sftps - define your sftp servers
* ftp - define your ftp servers

After that each and every server can have a list of upload and download pollers.

Example configuration:

```yaml
app:
  sftps:
    -
      name: localhostSFTP
      host: localhost
      port: 8888
      username: charis
      password: pass
    #  private-key: classpath:keys/user@ssh-cluster_id-dsa
    #  private-key-passphrase: changeit
      upload-pollers:
        -
          name: testSftpUploadPoller
          input-folder: D:\Temp\sftp-outbound-test\input
          processed-folder: D:\Temp\sftp-outbound-test\processed
          remote-output-folder: /sftp-outbound-test
          regex-filter: .+\.xml$
        -
          input-folder: D:\Temp\sftp-outbound-test\input\test2
          processed-folder: D:\Temp\sftp-outbound-test\processed\test2
          remote-folder: /sftp-outbound-test/test2
          regex-filter:
  ftps:
    -
      name: localhostFTP # arbitrary name, should be unique
      host: localhost
      port: 12345
      username: test
      password: test
      upload-pollers:
        -
          name: testFtpUploadPoller # arbitrary name, should be unique
          input-folder: D:\Temp\ftp-outbound-test\input # the directory from where to upload files
          processed-folder: D:\Temp\ftp-outbound-test\processed
          remote-output-folder: /ftp-outbound-test
          regex-filter: .+\.xml$
      download-pollers:
        -
          name: testFtpDownloadPoller
          download-folder: D:\Temp\ftp-outbound-test\downloading
          output-folder: D:\Temp\ftp-outbound-test\queue
          output-file-name-expression: "'bla_' + payload.lastModified() + payload.name" # Defaults to null which means original filename
          remote-input-folder: /ftp-input-test
          regex-filter: .+\.txt$
          delete-remote-file: false # default is true
          poll-interval-milliseconds: 30000 # 30s, defaults to 10s
          #poll-cron: 0 50 * * * ? # Run every 50th min every hour
          #modified-date-after-minutes: 5
```

### All available configuration

#### sftp section

```yaml
app:
  sftps:
    -
      name: # arbitrary name, should be unique
      host: # sftp host
      port: # sftp port
      username: # username
      password: # password
      private-key: # if ssh key enabled login here you put the path (classpath:, file:, http:)
      private-key-passphrase: # passphare for the key file if enabled
      upload-pollers:
        - # list of upload pollers
        ...
      download-pollers:
        - # list of upload pollers
        ...
```

#### ftp section

```yaml
app:
  ftps:
    -
      name: # arbitrary name, should be unique
      host: # ftp host
      port: # ftp port
      username: # username
      password: # password
      ftp-parser-date-format: # Optional: in case of legacy systems that do not respect the pattern for date format, e.g. 'dd/MM/yy HH:mm:ss'
      upload-pollers:
        - # list of upload pollers
        ...
      download-pollers:
        - # list of upload pollers
        ...
```

#### upload-pollers section

```yaml
app:
  sftps: # or ftp
    -
      upload-pollers:
        -
          name: # arbitrary name, should be unique
          input-folder: # the directory from where to upload files
          processed-folder: # successfully uploaded files will be moved here
          remote-output-folder: # remote directory where to upload files
          regex-filter: # regex for files in input folder
```

#### download-pollers section

```yaml
app:
  sftps: # or ftp
    -
      download-pollers:
        -
          name: # arbitrary name, should be unique
          download-folder: # the directory where the files will be placed temporary until they finish downloading
          output-folder: # the directory where the files will be placed when fully downloaded
          output-file-name-expression: # Optional: rename expression (SpEL) in case the files are not named uniquely
          remote-input-folder: # Remote directory where to listen for files to be downloaded
          regex-filter: # Remote file filter
          delete-remote-file: # default is true
          poll-interval-milliseconds: # defaults to 10s
          poll-cron: # Optional: in case cron is needed for polling instead of fixed interval
          semaphore-file-suffix: # Optional: it will wait for semaphore file before starting to download the file eg. '.sem'
          modified-date-after-minutes: 5 # Optional: This option will pick only pick up files that have last modified timestamp later than given time
```

#### storing to S3 instead of filesystem
```yaml
app:
  sftps: # or ftp
    -
      download-pollers:
        -
          ...
          s3-configuration:
            input-folder: ${java.io.tmpdir}/sftpClient/queue
            bucketName:
            aws-region:
            aws-account:
            aws-accessKey:
            aws-secretKey:
            aws-role:
            server-side-encryption: true
```
#### zipping section:

Where _input folder is the output of the download poller config_
```yaml  
app:
  sftps: # or ftp
    -
      download-pollers:
        -
          ...
          zip-configuration:
            min-items-to-zip: 100
            output-folder: ${java.io.tmpdir}/sftpClient/queue
            pattern: pictures-%s.tarz
```

#### log throughput:

Push throughput speeds for every file to ElasticSearch. Internally we are using RestHighLevelClient so it needs to be configured via Spring Boot.
```yaml  
spring:
  elasticsearch:
    rest:
      uris: localhost # Needed for RestHighLevelClient

app:
  es:
    index_pattern: "'data-exchange-client-' + T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('YYYY-MM'))"
```

### Notes while doing upload / download

When the app is uploading files it will suffix the original file being uploaded with **.uploading** and when the file is uploaded fully it will rename it back to the original file. So it is important to no start processing the file on the other end that has a suffix **.uploading**.

The upload and download pollers remember **in-memory** the **filename** and **created timestamp** of all the files polled and will not repeat them (**it will upload / download files only once**). Keep in mind if you **restart** the app the files **will be picked up again**.

### Monitoring

The application exposes HTTP monitoring endpoint to monitor which connections are UP or DOWN.

The default url is: http://localhost:8080/monitoring/connection-health-check
