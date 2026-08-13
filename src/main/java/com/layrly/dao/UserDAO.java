package com.layrly.dao;

import com.layrly.domain.User;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Data Access Object for users table
 * Handles all database operations related to users
 */
public class UserDAO extends BaseDAO {

    /**
     * Insert a new user into the database
     *
     * @param userName unique username
     * @param name     full name
     * @param email    email address
     * @param gender   gender
     * @param zip      zip code
     * @throws Exception if insertion fails
     */
    public void insertUser(UUID userName, String name, String email, String gender, String zip) throws Exception {
        executeTransaction(dynamoDb -> {
            Map<String, AttributeValue> itemValues = new HashMap<>();
            itemValues.put("user_name", AttributeValue.builder().s(userName.toString()).build());
            itemValues.put("name", AttributeValue.builder().s(name).build());
            itemValues.put("email", AttributeValue.builder().s(email).build());
            itemValues.put("gender", AttributeValue.builder().s(gender).build());
            itemValues.put("zip", AttributeValue.builder().s(zip).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName("users")
                    .item(itemValues)
                    .build();

            dynamoDb.putItem(request);

            System.out.println("User inserted successfully into DynamoDB.");
        });
    }

    /**
     * Check if a user exists by username
     *
     * @param userName username to check
     * @return true if user exists, false otherwise
     * @throws Exception if query fails
     */
    public boolean userExists(UUID userName) throws Exception {
        return executeQuery(dynamoDb -> {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("user_name", AttributeValue.builder().s(userName.toString()).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName("users")
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDb.getItem(request);

            if (response.hasItem()) {
                return true;
            }
            return false;
        });
    }

    /**
     * Get user by username
     *
     * @param userName username
     * @return User object or null if not found
     * @throws Exception if query fails
     */
    public User getUserByUsername(UUID userName) throws Exception {
        return executeQuery(dynamoDb -> {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("user_name", AttributeValue.builder().s(userName.toString()).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName("users")
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDb.getItem(request);

            if (response.hasItem()) {
                Map<String, AttributeValue> item = response.item();
                return new User(
                        UUID.fromString(item.get("user_name").s()),
                        item.get("name").s(),
                        item.get("email").s(),
                        item.get("gender").s(),
                        item.get("zip").s()
                );
            }
            return null;
        });
    }
}
