package ua.kharkiv.elsol.exporter.service.impl;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import ua.kharkiv.elsol.exporter.service.UploadService;

@Service
public class S3UploadService implements UploadService {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3UploadService.class);

  private final S3Client s3;

  private final String s3BucketName;

  public S3UploadService(S3Client s3Client, @Value("${s3.bucketName}") String s3BucketName) {
    this.s3 = s3Client;
    this.s3BucketName = s3BucketName;
  }

  @Override
  public boolean uploadFile(String pathToFile, String folderPath) {
    String keyName = folderPath + FilenameUtils.getName(pathToFile);
    CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
        .bucket(s3BucketName)
        .key(keyName)
        .build();

    CreateMultipartUploadResponse createResponse = s3.createMultipartUpload(createRequest);
    String uploadId = createResponse.uploadId();

    List<CompletedPart> completedParts = new ArrayList<>();
    int partNumber = 1;
    ByteBuffer buffer = ByteBuffer.allocate(5 * 1024 * 1024);

    try (RandomAccessFile file = new RandomAccessFile(pathToFile, "r")) {
      long fileSize = file.length();
      long position = 0;

      while (position < fileSize) {
        file.seek(position);
        int bytesRead = file.getChannel().read(buffer);

        buffer.flip();
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
            .bucket(s3BucketName)
            .key(keyName)
            .uploadId(uploadId)
            .partNumber(partNumber)
            .contentLength((long) bytesRead)
            .build();

        UploadPartResponse response = s3.uploadPart(uploadPartRequest,
            RequestBody.fromByteBuffer(buffer));

        completedParts.add(CompletedPart.builder()
            .partNumber(partNumber)
            .eTag(response.eTag())
            .build());

        buffer.clear();
        position += bytesRead;
        partNumber++;
      }
      CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
          .parts(completedParts)
          .build();

      CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
          .bucket(s3BucketName)
          .key(keyName)
          .uploadId(uploadId)
          .multipartUpload(completedUpload)
          .build();
      s3.completeMultipartUpload(completeRequest);
      LOGGER.info("The file was successfully uploaded to {}.", keyName);
      return true;
    } catch (IOException e) {
      LOGGER.error("Unable to upload file to S3. Reason: ", e);
      return false;
    }
  }
}
