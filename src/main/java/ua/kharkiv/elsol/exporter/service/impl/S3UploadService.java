package ua.kharkiv.elsol.exporter.service.impl;

import java.io.File;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import ua.kharkiv.elsol.exporter.service.UploadService;

@Service
public class S3UploadService implements UploadService {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3UploadService.class);

  private final MultipartS3AsyncClient s3;

  private final S3TransferManager s3TransferManager;

  private final String s3BucketName;

  public S3UploadService(MultipartS3AsyncClient s3Client, S3TransferManager s3TransferManager,
      @Value("${s3.bucketName}") String s3BucketName) {
    this.s3 = s3Client;
    this.s3TransferManager = s3TransferManager;
    this.s3BucketName = s3BucketName;
  }

  @Override
  public boolean uploadFile(String pathToFile, String folderPath) {
    String keyName = folderPath + FilenameUtils.getName(pathToFile);
    File file = new File(pathToFile);
    long contentLength = file.length();
    long partSize = 5 * 1024 * 1024;
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
//    CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
//        .bucket(s3BucketName)
//        .key(keyName)
//        .build();
//
//    CreateMultipartUploadResponse createResponse = s3.createMultipartUpload(createRequest);
//    String uploadId = createResponse.uploadId();
//
//    List<CompletedPart> completedParts = new ArrayList<>();
//    int partNumber = 1;
//    ByteBuffer buffer = ByteBuffer.allocate(5 * 1024 * 1024);
//    try (RandomAccessFile file = new RandomAccessFile(pathToFile, "r")) {
//      long fileSize = file.length();
//      long position = 0;
//
//      while (position < fileSize) {
//        file.seek(position);
//        int bytesRead = file.getChannel().read(buffer);
//
//        buffer.flip();
//        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
//            .bucket(s3BucketName)
//            .key(keyName)
//            .uploadId(uploadId)
//            .partNumber(partNumber)
//            .contentLength((long) bytesRead)
//            .build();
//
//        UploadPartResponse response = s3.uploadPart(uploadPartRequest,
//            RequestBody.fromByteBuffer(buffer));
//
//        completedParts.add(CompletedPart.builder()
//            .partNumber(partNumber)
//            .eTag(response.eTag())
//            .build());
//
//        buffer.clear();
//        position += bytesRead;
//        partNumber++;
//      }
//      CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
//          .parts(completedParts)
//          .build();
//
//      CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
//          .bucket(s3BucketName)
//          .key(keyName)
//          .uploadId(uploadId)
//          .multipartUpload(completedUpload)
//          .build();
//      s3.completeMultipartUpload(completeRequest);
//      LOGGER.info("The file was successfully uploaded to {}.", keyName);
//      IOUtils.closeQuietly(file);
//      return true;
//    } catch (IOException e) {
//      LOGGER.error("Unable to upload file to S3. Reason: ", e);
//      return false;
//    }
  }
}
