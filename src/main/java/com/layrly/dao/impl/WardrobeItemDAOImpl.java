package com.layrly.dao.impl;

import com.layrly.dao.WardrobeItemDAO;
import com.layrly.domain.WardrobeAnalyzedItem;
import com.layrly.domain.WardrobeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DynamoDB-backed implementation of {@link WardrobeItemDAO}.
 */
public class WardrobeItemDAOImpl extends BaseDAO implements WardrobeItemDAO {
    private static final Logger log = LoggerFactory.getLogger(WardrobeItemDAOImpl.class);

    private static final String apparelTableName = "apparel";
    private static final String apparelAnaylsisTableName = "apparel_analysis";

    @Override
    public void insertWardrobeItem(WardrobeItem item) throws Exception {
        executeTransaction(conn -> {
            insertWardrobeItem(item, item.analyzedItem().aiDescription(), conn);
        });
    }

    private static String insertWardrobeItem(WardrobeItem item, String aiDescription, DynamoDbClient dynamoDb) throws SQLException {
        String apparelId = UUID.randomUUID().toString();

        Map<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("apparel_id", AttributeValue.builder().s(apparelId).build());
        itemValues.put("user_name", AttributeValue.builder().s(item.userName().toString()).build());
        itemValues.put("image_url", AttributeValue.builder().s(item.fileName()).build());
        itemValues.put("category", AttributeValue.builder().s(item.category()).build());
        itemValues.put("color", AttributeValue.builder().s(item.color()).build());
        itemValues.put("brand", AttributeValue.builder().s(item.brand()).build());
        itemValues.put("ai_description", AttributeValue.builder().s(aiDescription).build());
        itemValues.put("created_at", AttributeValue.builder().s(Instant.now().toString()).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(apparelTableName)
                .item(itemValues)
                .build();

        dynamoDb.putItem(request);
        log.info("Wardrobe item inserted into DynamoDB.");

        return apparelId;
    }

    private void insertWardrobeAnalyzedItem(String itemId, String aiDescription, DynamoDbClient dynamoDb) throws SQLException {
        Map<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("analysis_id", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        itemValues.put("apparel_id", AttributeValue.builder().s(itemId).build());
        itemValues.put("ai_description", AttributeValue.builder().s(aiDescription).build());
        itemValues.put("created_at", AttributeValue.builder().s(Instant.now().toString()).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(apparelAnaylsisTableName)
                .item(itemValues)
                .build();

        dynamoDb.putItem(request);
        log.info("Wardrobe analyzed item inserted into DynamoDB.");
    }

    @Override
    public List<WardrobeItem> getWardrobeItemsByUserId(String userName) throws Exception {
        return executeQuery(dynamoDb -> {
            List<WardrobeItem> items = new ArrayList<>();
            QueryRequest apparelQuery = QueryRequest.builder()
                    .tableName(apparelTableName)
                    .keyConditionExpression("user_name = :userName")
                    .expressionAttributeValues(Map.of(":userName", AttributeValue.builder().s(userName).build()))
                    .scanIndexForward(false)
                    .build();

            return getWardrobeItems(dynamoDb, apparelQuery, items);
        });
    }

    @Override
    public List<WardrobeItem> getWardrobeItemsByUserNameAndCategory(String userName, String category) throws Exception {
        return executeQuery(dynamoDb -> {
            List<WardrobeItem> items = new ArrayList<>();
            QueryRequest apparelQuery = QueryRequest.builder()
                    .tableName("apparel")
                    .keyConditionExpression("user_name = :userName")
                    .filterExpression("category = :category")
                    .expressionAttributeValues(Map.of(
                            ":userName", AttributeValue.builder().s(userName).build(),
                            ":category", AttributeValue.builder().s(category).build()
                    ))
                    .build();

            return getWardrobeItems(dynamoDb, apparelQuery, items);
        });
    }

    @Override
    public void updateWardrobeItem(String apparelId, String category, String color, String brand, UUID userName) throws Exception {
        executeTransaction(dynamoDb -> {
            try {
                Map<String, AttributeValue> key = Map.of(
                        "user_name", AttributeValue.builder().s(userName.toString()).build(),
                        "apparel_id", AttributeValue.builder().s(apparelId).build()
                );

                Map<String, AttributeValue> values = Map.of(
                        ":category", AttributeValue.builder().s(category).build(),
                        ":color", AttributeValue.builder().s(color).build(),
                        ":brand", AttributeValue.builder().s(brand).build(),
                        ":modifiedAt", AttributeValue.builder().s(Instant.now().toString()).build()
                );

                UpdateItemRequest request = UpdateItemRequest.builder()
                        .tableName(apparelTableName)
                        .key(key)
                        .updateExpression(
                                "SET category = :category, " +
                                        "color = :color, " +
                                        "brand = :brand, " +
                                        "modified_at = :modifiedAt"
                        )
                        .expressionAttributeValues(values)
                        .conditionExpression("attribute_exists(user_name) AND attribute_exists(apparel_id)")
                        .build();

                dynamoDb.updateItem(request);
                log.info("Wardrobe item updated. ID: " + apparelId);
            } catch (ConditionalCheckFailedException e) {
                throw new Exception("Wardrobe item not found or you do not have permission to update.", e);
            }
        });
    }

    @Override
    public void deleteWardrobeItem(String id, UUID userName) throws Exception {
        executeTransaction(dynamoDb -> {
            try {
                Map<String, AttributeValue> key = Map.of(
                        "user_name", AttributeValue.builder().s(userName.toString()).build(),
                        "apparel_id", AttributeValue.builder().s(id).build()
                );

                DeleteItemRequest request = DeleteItemRequest.builder()
                        .tableName(apparelTableName)
                        .key(key)
                        .conditionExpression("attribute_exists(user_name) AND attribute_exists(apparel_id)")
                        .build();

                dynamoDb.deleteItem(request);
            } catch (ConditionalCheckFailedException e) {
                throw new Exception("Wardrobe item not found or you do not have permission to delete.", e);
            }
        });
    }

    @Override
    public int getApparelCountByUserName(String userName) throws Exception {
        return executeQuery(dynamoDb -> {
            QueryRequest request = QueryRequest.builder()
                    .tableName(apparelTableName)
                    .keyConditionExpression("user_name = :user_name")
                    .expressionAttributeValues(Map.of(":user_name", AttributeValue.builder().s(userName).build()))
                    .select("COUNT")
                    .build();

            return dynamoDb.query(request).count();
        });
    }

    private static List<WardrobeItem> getWardrobeItems(DynamoDbClient dynamoDb, QueryRequest apparelQuery, List<WardrobeItem> items) {
        QueryResponse apparelResponse = dynamoDb.query(apparelQuery);
        List<Map<String, AttributeValue>> apparelItems = apparelResponse.items();
        List<String> apparelIds = apparelItems.stream()
                .map(item -> item.get("apparel_id").s())
                .toList();

        if (apparelIds.isEmpty()) {
            return items;
        }

        for (Map<String, AttributeValue> apparelItem : apparelItems) {
            items.add(new WardrobeItem(
                    apparelItem.get("apparel_id").s(),
                    UUID.fromString(apparelItem.get("user_name").s()),
                    apparelItem.get("image_url").s(),
                    apparelItem.get("category").s(),
                    apparelItem.get("color").s(),
                    apparelItem.get("brand").s(),
                    new WardrobeAnalyzedItem(apparelItem.get("apparel_id").s(), apparelItem.get("ai_description").s())
            ));
        }

        return items;
    }
}
