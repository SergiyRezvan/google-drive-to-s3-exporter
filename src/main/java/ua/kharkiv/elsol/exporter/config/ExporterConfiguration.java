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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.internal.crossregion.endpointprovider.BucketEndpointProvider;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class ExporterConfiguration {

  @Value("${sm.google.drive.creds}")
  private String secretsManagerCreds;

  @Value("${aws.region}")
  private String region;

  @Value("${s3.bucketName}")
  private String bucketName;

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
  public S3Client s3Client() {
    S3ClientBuilder clientBuilder = S3Client.builder();
    try (final S3Client client = clientBuilder.region(Region.of(region)).build()) {
      final GetBucketLocationResponse response = client.getBucketLocation(
          GetBucketLocationRequest.builder().bucket(bucketName).build());
      clientBuilder.region(Region.of(response.locationConstraintAsString()));
    }
    return clientBuilder.build();
  }

  @Bean
  public SnsClient snsClient() {
    return SnsClient.builder().region(Region.of(region)).build();
  }

  @Bean
  public DynamoDbClient dynamoDbClient() {
    return DynamoDbClient.builder().region(Region.of(region)).build();
  }

}
