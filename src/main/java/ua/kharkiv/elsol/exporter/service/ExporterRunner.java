package ua.kharkiv.elsol.exporter.service;

import com.google.api.services.drive.model.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ExporterRunner implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExporterRunner.class);

  private final GoogleDriveService googleDriveService;

  private final UploadService uploadService;

  private final NotificationService notificationService;

  private final DataStoreService dataStoreService;

  public ExporterRunner(GoogleDriveService googleDriveService, UploadService uploadService,
      NotificationService notificationService, DataStoreService dataStoreService) {
    this.googleDriveService = googleDriveService;
    this.uploadService = uploadService;
    this.notificationService = notificationService;
    this.dataStoreService = dataStoreService;
  }

  @Override
  public void run(String... args) throws Exception {
    LOGGER.info("Start exporter");
    String latestExportRun = dataStoreService.getLatestExportRun();
    String currentExportRunTime = Instant.now().toString();
    List<File> allFiles = googleDriveService.getAllFiles(latestExportRun);
    AtomicInteger successfulNumberOfFiles = new AtomicInteger();
    AtomicInteger failedNumberOfFiles = new AtomicInteger();
    AtomicInteger skippedFiles = new AtomicInteger();
    allFiles.forEach(file -> {
      LOGGER.info("file name: {}, mimeType: {}, modifiedTime: {}",
          file.getName(), file.getMimeType(), file.getModifiedTime());
      try {
        var filePath = googleDriveService.downloadFile(file);
        if (filePath != null) {
          LOGGER.info("File {}, was downloaded to: {}", file.getName(), filePath);
          String folderPath = getFolderPath(file);
          uploadService.uploadFile(filePath, folderPath);
          Thread.sleep(1000);
          successfulNumberOfFiles.incrementAndGet();
          Files.delete(Path.of(filePath));
          LOGGER.info("File {} was cleaned up.", filePath);
        } else {
          skippedFiles.incrementAndGet();
        }
      } catch (Exception ex) {
        LOGGER.warn("Unable to export file {}, reason: {}", file.getName(), ex);
        failedNumberOfFiles.incrementAndGet();
      }
    });
    String notificationMessage = MessageFormat.format(
        "Export of google drive has finished. Total number of files: {0}, successfully exported: {1}, failed to export: {2}, skipped: {3}",
        allFiles.size(), successfulNumberOfFiles.get(), failedNumberOfFiles.get(),
        skippedFiles.get());
    notificationService.sendNotification(notificationMessage);
    dataStoreService.updateLatestExportRun(currentExportRunTime);
    LOGGER.info("Finish file exporter with the result: {}", notificationMessage);
  }

  private String getFolderPath(File file) throws Exception {
    if (file.getParents() != null && !file.getParents().isEmpty()) {
      return getFullFolderPath(file.getParents().get(0));
    }
    return "";
  }

  public String getFullFolderPath(String fileId) throws Exception {
    String folderPath = "/";
    File file;
    do {
      file = googleDriveService.getParent(fileId);
      folderPath = "/" + file.getName() + folderPath;
      if (!Objects.isNull(file.getParents()) && !file.getParents().isEmpty()) {
        fileId = file.getParents().get(0);
      } else {
        fileId = null;
      }
    } while (!Objects.isNull(fileId));
    return folderPath.replaceAll(" ", "_");
  }

}
