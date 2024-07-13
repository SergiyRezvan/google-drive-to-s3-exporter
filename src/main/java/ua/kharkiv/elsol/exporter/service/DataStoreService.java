package ua.kharkiv.elsol.exporter.service;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

@Service
public class DataStoreService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataStoreService.class);

  private final DynamoDbClient dynamoDbClient;

  private final String tableName;

  public DataStoreService(DynamoDbClient dynamoDbClient,
      @Value("${dynamodb.table.name}") String tableName) {
    this.dynamoDbClient = dynamoDbClient;
    this.tableName = tableName;
  }

  public String getLatestExportRun() {
    Map<String, AttributeValue> keyToGet = new HashMap<>();
    keyToGet.put("execution", AttributeValue.builder()
        .s("last")
        .build());
    GetItemRequest readRequest = GetItemRequest.builder()
        .tableName(tableName)
        .key(keyToGet)
        .build();
    Map<String, AttributeValue> item = dynamoDbClient.getItem(readRequest).item();
    if (item.isEmpty()) {
      LOGGER.info("There is no record in dynamoDbTable. All items will be exported");
      return null;
    }
    String lastExecution = item.get("time").s();
    LOGGER.info("Items starting from {} will be exported", lastExecution);
    return lastExecution;
  }

  public void updateLatestExportRun(String latestTimestamp) {
    Map<String, AttributeValue> itemValues = new HashMap<>();
    itemValues.put("execution", AttributeValue.builder().s("last").build());
    itemValues.put("time", AttributeValue.builder().s(latestTimestamp).build());
    PutItemRequest request = PutItemRequest.builder()
        .tableName(tableName)
        .item(itemValues)
        .build();
    try {
      PutItemResponse response = dynamoDbClient.putItem(request);
      LOGGER.info("Successfully updated last execution run to {}", latestTimestamp);
    } catch (Exception ex) {
      LOGGER.error("Unable to update dynamo db with latest execution run", ex);
    }
  }

}
