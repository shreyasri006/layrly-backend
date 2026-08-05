package com.layrly.dao;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object for login_history table
 * Handles all database operations related to user login
 */
public class LoginHistoryDAO extends BaseDAO {
    public void insert(UUID userName) throws Exception {
        executeTransaction(dynamoDb -> {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build()); // Generate unique ID
            item.put("user_name", AttributeValue.builder().s(userName.toString()).build());
            item.put("login_time", AttributeValue.builder().s(Instant.now().toString()).build()); // Add current timestamp

            PutItemRequest request = PutItemRequest.builder()
                    .tableName("login_history")
                    .item(item)
                    .build();

            dynamoDb.putItem(request);

            System.out.println("User login inserted successfully into DynamoDB.");
        });
    }

    /**
     * Get continuous login streak count for a given user name
     * Returns the number of consecutive days the user has logged in, starting from today or yesterday.
     * If there is no login today or yesterday, returns 0.
     * Examples: 3 days streak, 2 days streak, 1 day streak, 5 days streak
     *
     * @param userName username (UUID)
     * @return count of consecutive login days; 0 if no login in last 2 days
     * @throws Exception if query fails
     */
    public long getRecentLoginsByUserName(UUID userName) throws Exception {

        return executeQuery(dynamoDb -> {
            // Calculate consecutive login days starting from today or yesterday
            // Query DynamoDB for login records of the given user
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName("login_history")
                    .keyConditionExpression("user_name = :user_name")
                    .expressionAttributeValues(
                            Map.of(":user_name", AttributeValue.builder().s(userName.toString()).build())
                    )
                    .projectionExpression("login_time")
                    .limit(50)
                    .build();

            QueryResponse response = dynamoDb.query(queryRequest);

            // Extract and sort distinct login dates
            Set<LocalDate> loginDates = response.items().stream()
                    .map(item -> LocalDate.parse(item.get("login_time").s().substring(0, 10))) // Extract date part
                    .collect(Collectors.toCollection(TreeSet::new)); // TreeSet ensures sorted order

            // Calculate consecutive login streak
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            if (!loginDates.contains(today) && !loginDates.contains(yesterday)) {
                return 0L; // No login today or yesterday
            }

            long streak = 0;
            List<LocalDate> descendingDates = new ArrayList<>(loginDates);
            descendingDates.sort(Comparator.reverseOrder());

            for (LocalDate date : descendingDates) {
                if (date.equals(today) || date.equals(yesterday) || date.equals(today.minusDays(streak))) {
                    streak++;
                } else {
                    break;
                }
            }

            return streak;
        });
    }
}
