package ua.kharkiv.elsol.exporter.service.impl;

import java.io.File;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import ua.kharkiv.elsol.exporter.service.UploadService;

@Service
public class S3UploadService implements UploadService {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3UploadService.class);

  private final S3TransferManager s3TransferManager;

  private final String s3BucketName;

  public S3UploadService(S3TransferManager s3TransferManager,
      @Value("${s3.bucketName}") String s3BucketName) {
    this.s3TransferManager = s3TransferManager;
    this.s3BucketName = s3BucketName;
  }

  @Override
  public boolean uploadFile(String pathToFile, String folderPath) {
    String keyName = folderPath + FilenameUtils.getName(pathToFile);
    try {
      UploadFileRequest fileRequest = UploadFileRequest.builder()
          .putObjectRequest(PutObjectRequest.builder().bucket(s3BucketName).key(keyName).build())
          .source(new File(pathToFile))
          .build();
      FileUpload upload = s3TransferManager.uploadFile(fileRequest);
      upload.completionFuture().get();
      return true;
    } catch (Exception ex) {
      LOGGER.error("Unable to upload file to S3. Reason: ", ex);
      return false;
    }
  }
}
