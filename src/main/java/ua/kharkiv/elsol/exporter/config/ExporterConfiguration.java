package ua.kharkiv.elsol.exporter.config;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClient;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.util.StringInputStream;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Configuration
public class ExporterConfiguration {

  @Value("${sm.google.drive.creds}")
  private String secretsManagerCreds;

  @Value("${aws.region}")
  private String region;

  @Bean
  public HttpTransport httpTransport() throws Exception {
    return GoogleNetHttpTransport.newTrustedTransport();
  }

  @Bean
  public JsonFactory jsonFactory() {
    return GsonFactory.getDefaultInstance();
  }

  @Bean
  public HttpRequestInitializer httpRequestInitializer() throws Exception {
    AWSSecretsManager secretsManagerClient = AWSSecretsManagerClient.builder().build();
    GetSecretValueRequest secretValueRequest = new GetSecretValueRequest();
    secretValueRequest.setSecretId(secretsManagerCreds);
    GetSecretValueResult secretValue = secretsManagerClient.getSecretValue(secretValueRequest);
    var credentials = ServiceAccountCredentials.fromStream(
            new StringInputStream(secretValue.getSecretString()))
        .createScoped(DriveScopes.all());
    return new HttpCredentialsAdapter(credentials);
  }

  @Bean
  public Drive drive() throws Exception {
    return new Drive.Builder(httpTransport(), jsonFactory(), httpRequestInitializer())
        .setApplicationName("GoogleDiscExporter").build();
  }

  @Bean
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("folder");
  }

  @Bean
  public S3AsyncClient s3Client() {
    return S3AsyncClient.builder()
        .region(Region.US_EAST_2)
        .endpointOverride(URI.create("https://s3.us-east-2.amazonaws.com"))
        .build();
  }

  @Bean
  public MultipartS3AsyncClient multipartS3AsyncClient() {
    return MultipartS3AsyncClient.create(s3Client(), MultipartConfiguration.builder()
        .minimumPartSizeInBytes(5 * 1024 * 1024L).build());
  }

  @Bean
  public S3TransferManager s3TransferManager() {
    return S3TransferManager.builder().s3Client(s3Client()).build();
  }

  @Bean
  public SnsClient snsClient() {
    return SnsClient.builder().region(Region.US_EAST_2).build();
  }

  @Bean
  public DynamoDbClient dynamoDbClient() {
    return DynamoDbClient.builder().region(Region.US_EAST_2).build();
  }

}
