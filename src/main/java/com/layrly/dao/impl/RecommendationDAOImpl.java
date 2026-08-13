package com.layrly.dao.impl;

import com.layrly.dao.RecommendationDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB-backed implementation of {@link RecommendationDAO}.
 */
public class RecommendationDAOImpl extends BaseDAO implements RecommendationDAO {
    private static final Logger log = LoggerFactory.getLogger(RecommendationDAOImpl.class);
    private static final String TABLE_NAME = "recommendations";

    @Override
    public void insert(String userName, String context, String outfits, String model) throws Exception {
        executeTransaction(dynamoDb -> {
            String createdAt = Instant.now().toString();
            Map<String, AttributeValue> item = new HashMap<>();

            item.put("user_name", AttributeValue.builder().s(userName).build());
            item.put("created_at", AttributeValue.builder().s(createdAt).build());
            item.put("context", AttributeValue.builder().s(context).build());
            item.put("outfits", AttributeValue.builder().s(outfits).build());
            item.put("model_version", AttributeValue.builder().s(model).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build();

            dynamoDb.putItem(request);
            log.info("Recommendation inserted successfully.");
        });
    }

    @Override
    public String getLatestOutFitByUserNameAndCreatedTime(String userName, int hours) throws Exception {
        return executeQuery(dynamoDb -> {
            Instant cutoff = Instant.now().minusSeconds(hours * 60L * 60L);

            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("user_name = :userName AND created_at > :cutoff")
                    .expressionAttributeValues(Map.of(
                            ":userName", AttributeValue.builder().s(userName).build(),
                            ":cutoff", AttributeValue.builder().s(cutoff.toString()).build()
                    ))
                    .projectionExpression("outfits")
                    .build();

            QueryResponse response = dynamoDb.query(request);
            if (response.count() > 0) {
                return response.items().get(0).get("outfits").s();
            }

            return null;
        });
    }

    @Override
    public int getTotalRecommendationsCountByUserName(String userName) throws Exception {
        return executeQuery(dynamoDb -> {
            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("user_name = :userName")
                    .expressionAttributeValues(Map.of(
                            ":userName", AttributeValue.builder().s(userName).build()
                    ))
                    .select("COUNT")
                    .build();

            return dynamoDb.query(request).count();
        });
    }
}
