package com.layrly.dao.impl;

import com.layrly.dao.LoginHistoryDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * DynamoDB-backed implementation of {@link LoginHistoryDAO}.
 */
public class LoginHistoryDAOImpl extends BaseDAO implements LoginHistoryDAO {
    private static final Logger log = LoggerFactory.getLogger(LoginHistoryDAOImpl.class);

    @Override
    public void insert(String userName) throws Exception {
        executeTransaction(dynamoDb -> {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().n(String.valueOf(System.nanoTime())).build());
            item.put("user_name", AttributeValue.builder().s(userName).build());
            item.put("login_time", AttributeValue.builder().s(Instant.now().toString()).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName("login_history")
                    .item(item)
                    .build();

            dynamoDb.putItem(request);
            log.info("User login inserted successfully into DynamoDB.");
        });
    }

    @Override
    public int getRecentLoginsByUserName(String userName) throws Exception {
        return executeQuery(dynamoDb -> {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName("login_history")
                    .indexName("idx_login_user")
                    .keyConditionExpression("user_name = :user_name")
                    .expressionAttributeValues(Map.of(":user_name", AttributeValue.builder().s(userName).build()))
                    .projectionExpression("login_time")
                    .scanIndexForward(false)
                    .limit(50)
                    .build();

            QueryResponse response = dynamoDb.query(queryRequest);

            Set<LocalDate> loginDates = response.items().stream()
                    .map(item -> LocalDate.parse(item.get("login_time").s().substring(0, 10)))
                    .collect(Collectors.toCollection(TreeSet::new));

            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            if (!loginDates.contains(today) && !loginDates.contains(yesterday)) {
                return 0;
            }

            int streak = 0;
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
