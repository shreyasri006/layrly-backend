package com.layrly.dao;

import com.layrly.domain.WardrobeAnalyzedItem;
import com.layrly.domain.WardrobeItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/**
 * Data Access Object for Wardrobe Items table
 */
public class WardrobeItemDAO extends BaseDAO {
    private static String apparelTableName = "apparel";
    private static String apparelAnaylsisTableName = "apparel_analysis";


    /**
     * Insert a new wardrobe item
     */
    public void insertWardrobeItem(WardrobeItem item) throws Exception {
        executeTransaction(conn -> {
            String itemId = insertWardrobeItem(item, item.analyzedItem().aiDescription(), conn);
            //insertWardrobeAnalyzedItem(itemId, item.analyzedItem().aiDescription(), conn);
        });
    }

    private static String insertWardrobeItem(WardrobeItem item, String aiDescription, DynamoDbClient dynamoDb) throws SQLException {
        String apparelId = UUID.randomUUID().toString();

        // Create item attributes
        Map<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("apparel_id", AttributeValue.builder().s(apparelId).build());
        itemValues.put("user_name", AttributeValue.builder().s(item.userName().toString()).build());
        itemValues.put("image_url", AttributeValue.builder().s(item.fileName()).build());
        itemValues.put("category", AttributeValue.builder().s(item.category()).build());
        itemValues.put("color", AttributeValue.builder().s(item.color()).build());
        itemValues.put("brand", AttributeValue.builder().s(item.brand()).build());
        itemValues.put("ai_description", AttributeValue.builder().s(aiDescription).build());
        itemValues.put("created_at", AttributeValue.builder().s(Instant.now().toString()).build());

        // Build the PutItemRequest
        PutItemRequest request = PutItemRequest.builder()
                .tableName(apparelTableName)
                .item(itemValues)
                .build();

        // Execute the PutItem operation
        dynamoDb.putItem(request);
        System.out.println("Wardrobe item inserted into DynamoDB.");

        return apparelId;
    }

    private void insertWardrobeAnalyzedItem(String itemId, String aiDescription, DynamoDbClient dynamoDb) throws SQLException {

        // Create item attributes
        Map<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("analysis_id", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        itemValues.put("apparel_id", AttributeValue.builder().s(itemId).build());
        itemValues.put("ai_description", AttributeValue.builder().s(aiDescription).build());
        itemValues.put("created_at", AttributeValue.builder().s(Instant.now().toString()).build());

        // Build the PutItemRequest
        PutItemRequest request = PutItemRequest.builder()
                .tableName(apparelAnaylsisTableName)
                .item(itemValues)
                .build();

        // Execute the PutItem operation
        dynamoDb.putItem(request);
        System.out.println("Wardrobe analyzed item inserted into DynamoDB.");
    }


    /**
     * Get all wardrobe items for a user with most recent items first
     */
    public List<WardrobeItem> getWardrobeItemsByUserId(String userName) throws Exception {                 //11
        return executeQuery(dynamoDb -> {
            List<WardrobeItem> items = new ArrayList<>();

            // Step 1: Query the apparel table
            QueryRequest apparelQuery = QueryRequest.builder()
                    .tableName(apparelTableName)
                    .keyConditionExpression("user_name = :userName")
                    .expressionAttributeValues(Map.of(":userName", AttributeValue.builder().s(userName).build()))
                    .scanIndexForward(false) // Order by descending
                    .build();

            return getWardrobeItems(dynamoDb, apparelQuery, items);
        });
    }

    /**
     * Get all wardrobe items for a user by category with most recent items first
     */
    public List<WardrobeItem> getWardrobeItemsByUserNameAndCategory(String userName, String category) throws Exception { //11
        return executeQuery(dynamoDb -> {
            List<WardrobeItem> items = new ArrayList<>();

            // Step 1: Query the apparel table
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

    /**
     * Update a wardrobe item by user name
     */
    public void updateWardrobeItem(String apparelId, String category, String color, String brand, UUID userName) throws Exception {
        executeTransaction(dynamoDb -> {
            try {
                Map<String, AttributeValue> key = Map.of(
                        "user_name", AttributeValue.builder()
                                .s(userName.toString())
                                .build(),
                        "apparel_id", AttributeValue.builder()
                                .s(apparelId)
                                .build()
                );

                Map<String, AttributeValue> values = Map.of(
                        ":category", AttributeValue.builder().s(category).build(),
                        ":color", AttributeValue.builder().s(color).build(),
                        ":brand", AttributeValue.builder().s(brand).build(),
                        ":modifiedAt", AttributeValue.builder()
                                .s(Instant.now().toString())
                                .build()
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
                        .conditionExpression(
                                "attribute_exists(user_name) AND attribute_exists(apparel_id)"
                        )
                        .build();

                dynamoDb.updateItem(request);

                System.out.println("Wardrobe item updated. ID: " + apparelId);

            } catch (ConditionalCheckFailedException e) {
                throw new Exception(
                        "Wardrobe item not found or you do not have permission to update.",
                        e
                );
            }
//            String sql = "UPDATE apparel SET category = ?, color = ?, brand = ?, modified_at = CURRENT_TIMESTAMP WHERE apparel_id = ? AND user_name = ?";
//
//            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
//                stmt.setString(1, category);
//                stmt.setString(2, color);
//                stmt.setString(3, brand);
//                stmt.setLong(4, id);
//                stmt.setObject(5, userName);
//
//                int rowsAffected = stmt.executeUpdate();
//                if(rowsAffected == 0) {
//                    throw new Exception("Wardrobe item not found or you do not have permission to update.");
//                }
//                System.out.println("Wardrobe item updated. Rows affected: " + rowsAffected);
//            }
        });
    }

    /**
     * Delete a wardrobe item by id and userName (ownership validation)
     *
     * @param id       wardrobe item id
     * @param userName user name (for security validation)
     * @throws Exception if deletion fails
     */
    public void deleteWardrobeItem(String id, UUID userName) throws Exception {
        executeTransaction(dynamoDb -> {
            try {
                Map<String, AttributeValue> key = Map.of(
                        "user_name", AttributeValue.builder()
                                .s(userName.toString())
                                .build(),
                        "apparel_id", AttributeValue.builder()
                                .s(id)
                                .build()
                );

                DeleteItemRequest request = DeleteItemRequest.builder()
                        .tableName("apparel")
                        .key(key)
                        .conditionExpression(
                                "attribute_exists(user_name) AND attribute_exists(apparel_id)"
                        )
                        .build();

                dynamoDb.deleteItem(request);

            } catch (ConditionalCheckFailedException e) {
                throw new Exception(
                        "Wardrobe item not found or you do not have permission to delete.",
                        e
                );
            }
        });
    }

    private void deleteApparelAnalysis(long id, DynamoDbClient dynamoDb) throws Exception {
        String sql = "DELETE FROM apparel_analysis WHERE apparel_id = ?";

//        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setLong(1, id);
//
//            int rowsAffected = stmt.executeUpdate();
//            if(rowsAffected == 0) {
//                throw new Exception("Wardrobe item not found.");
//            }
//            System.out.println("Wardrobe Analysis item deleted. Rows affected: " + rowsAffected);
//        }
    }

    private static void deleteApparel(long id, UUID userName, DynamoDbClient dynamoDb) throws Exception {
        String sql = "DELETE FROM apparel WHERE apparel_id = ? AND user_name = ?";

//        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setLong(1, id);
//            stmt.setObject(2, userName);
//
//            int rowsAffected = stmt.executeUpdate();
//            if(rowsAffected == 0) {
//                throw new Exception("Wardrobe item not found or you do not have permission to delete.");
//            }
//            System.out.println("Wardrobe item deleted. Rows affected: " + rowsAffected);
//        }
    }

    /**
     * Get total count of apparel records for a given user name
     *
     * @param userName user name (UUID)
     * @return count of apparel records
     * @throws Exception if query fails
     */
    public long getApparelCountByUserName(UUID userName) throws Exception { //11
        return executeQuery(dynamoDb -> {
//            String sql = "SELECT COUNT(*) as count FROM apparel WHERE user_name = ?";
//
//            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
//                stmt.setObject(1, userName);
//
//                try (ResultSet rs = stmt.executeQuery()) {
//                    if(rs.next()) {
//                        return rs.getLong("count");
//                    }
//                }
//            }
            return 0L;
        });
    }

    private static List<WardrobeItem> getWardrobeItems(DynamoDbClient dynamoDb, QueryRequest apparelQuery, List<WardrobeItem> items) {
        QueryResponse apparelResponse = dynamoDb.query(apparelQuery);

        // Collect all apparel items and their IDs
        List<Map<String, AttributeValue>> apparelItems = apparelResponse.items();
        List<String> apparelIds = apparelItems.stream()
                .map(item -> item.get("apparel_id").s())
                .toList();

        if (apparelIds.isEmpty()) {
            return items;
        }

//        // Step 2: BatchGetItem for apparel_analysis
//        List<Map<String, AttributeValue>> keys = apparelIds.stream()
//                .map(id -> Map.of("apparel_id", AttributeValue.builder().s(id).build()))
//                .toList();
//
//        System.out.println("keys: " + keys);
//
//        BatchGetItemRequest batchRequest = BatchGetItemRequest.builder()
//                .requestItems(Map.of(
//                        apparelAnaylsisTableName,
//                        KeysAndAttributes.builder().keys(keys).build()
//                ))
//                .build();
//
//        BatchGetItemResponse batchResponse = dynamoDb.batchGetItem(batchRequest);
//
//        // Map apparel_id to ai_description
//        Map<String, String> analysisMap = batchResponse.responses().get(apparelAnaylsisTableName).stream()
//                .collect(Collectors.toMap(
//                        item -> item.get("apparel_id").s(),
//                        item -> item.get("ai_description").s()
//                ));

        // Step 3: Combine results
        for (Map<String, AttributeValue> apparelItem : apparelItems) {
//            String apparelId = apparelItem.get("apparel_id").s();
//            String aiDescription = analysisMap.get(apparelId);

            items.add(new WardrobeItem(
                    apparelItem.get("apparel_id").s(),
                    UUID.fromString(apparelItem.get("user_name").s()),
                    apparelItem.get("image_url").s(),
                    apparelItem.get("category").s(),
                    apparelItem.get("color").s(),
                    apparelItem.get("brand").s(),
                    new WardrobeAnalyzedItem(null, apparelItem.get("ai_description").s())
            ));
        }

        return items;
    }
}
