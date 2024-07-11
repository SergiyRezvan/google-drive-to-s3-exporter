package ua.kharkiv.elsol.exporter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class NotificationService {

  private final SnsClient snsClient;

  private final String snsTopic;

  public NotificationService(SnsClient snsClient, @Value("${sns.topic.arn}") String snsTopic) {
    this.snsClient = snsClient;
    this.snsTopic = snsTopic;
  }

  public void sendNotification(String message) {
    PublishRequest request = PublishRequest.builder()
        .message(message)
        .topicArn(snsTopic)
        .build();
    snsClient.publish(request);
  }

}
