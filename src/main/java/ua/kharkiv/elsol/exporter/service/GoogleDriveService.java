package ua.kharkiv.elsol.exporter.service;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ua.kharkiv.elsol.exporter.utils.Utils;

@Service
public class GoogleDriveService {

  private static final Logger LOGGER = LoggerFactory.getLogger(GoogleDriveService.class);

  private final Drive driveClient;
  private final HttpTransport httpTransport;

  @Value("${supported.export.types}")
  private List<String> supportedExportTypes;

  @Value("${default.export.type}")
  private String defaultExportType;

  public GoogleDriveService(Drive driveClient, HttpTransport httpTransport) {
    this.driveClient = driveClient;
    this.httpTransport = httpTransport;
  }

  public List<File> getAllFiles(String modifiedTimeFrom) throws Exception {
    List<File> result = new ArrayList<File>();
    var query = "trashed = false and mimeType != 'application/vnd.google-apps.folder'";
    if (StringUtils.hasText(modifiedTimeFrom)) {
      query += " and modifiedTime >= '" + modifiedTimeFrom + "'";
    }
    Drive.Files.List request = driveClient.files().list()
        .setQ(query)
        .setFields("files(id, name, mimeType, modifiedTime, createdTime, parents, exportLinks)")
        .setPageSize(20);
    do {
      try {
        FileList files = request.execute();
        result.addAll(files.getFiles());
        request.setPageToken(files.getNextPageToken());
      } catch (IOException e) {
        LOGGER.error("An error occurred:", e);
        request.setPageToken(null);
      }
    } while (request.getPageToken() != null &&
        request.getPageToken().length() > 0);
    return result;
  }

  @Cacheable("folder")
  public File getParent(String parentFolderId) throws Exception {
    LOGGER.info("Request folder info for {}", parentFolderId);
    var request = driveClient.files().get(parentFolderId)
        .setFields("id, name, parents");
    return request.execute();
  }

  public String downloadFile(File fileToExport) throws Exception {
    java.io.File parentDir = new java.io.File(System.getProperty("user.home"));
    String downloadedFile = parentDir + java.io.File.separator
        + fileToExport.getName();
    if (fileToExport.getMimeType().startsWith("application/vnd.google-apps")) {
      String exportedMimeType = fileToExport.getExportLinks().keySet().stream()
          .filter(mimeType -> supportedExportTypes.contains(mimeType)).findFirst()
          .orElse(defaultExportType);
      String exportedLink = fileToExport.getExportLinks().get(exportedMimeType);
      downloadedFile += "." + Utils.getFileExtension(exportedLink);
      OutputStream out = new FileOutputStream(downloadedFile);
      var downloader = new MediaHttpDownloader(httpTransport,
          driveClient.getRequestFactory().getInitializer());
      downloader.download(new GenericUrl(exportedLink), out);
    } else {
      OutputStream out = new FileOutputStream(downloadedFile);
      var request = driveClient.files().get(fileToExport.getId());
      request.executeMediaAndDownloadTo(out);
      return downloadedFile;
    }
    return downloadedFile;
  }

}
