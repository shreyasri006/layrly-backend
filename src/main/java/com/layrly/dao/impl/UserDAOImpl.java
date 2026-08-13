package com.layrly.dao.impl;

import com.layrly.dao.UserDAO;
import com.layrly.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DynamoDB-backed implementation of {@link UserDAO}.
 */
public class UserDAOImpl extends BaseDAO implements UserDAO {
    private static final Logger log = LoggerFactory.getLogger(UserDAOImpl.class);

    @Override
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
            log.info("User inserted successfully into DynamoDB.");
        });
    }

    @Override
    public boolean userExists(UUID userName) throws Exception {
        return executeQuery(dynamoDb -> {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("user_name", AttributeValue.builder().s(userName.toString()).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName("users")
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDb.getItem(request);
            return response.hasItem();
        });
    }

    @Override
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
