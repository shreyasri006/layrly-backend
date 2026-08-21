package com.layrly.dao.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Abstract base class for all Data Access Objects
 * Provides common database operations for specific tables
 */
public abstract class BaseDAO {
    private static final Logger log = LoggerFactory.getLogger(BaseDAO.class);
    private static final String region = System.getenv("AWS_REGION");
    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(Region.of(region != null ? region : "us-east-2"))
            .httpClient(UrlConnectionHttpClient.create())
            .build();

    /**
     * Execute a database operation with automatic transaction management
     *
     * @param operation the database operation to execute
     * @throws Exception if the operation fails
     */
    protected void executeTransaction(DatabaseOperation operation) throws Exception {
        try {
            operation.execute(dynamoDb);
        } catch (Exception e) {
            log.error("Database transaction failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Execute a database query with automatic resource management
     *
     * @param queryOperation the database query to execute
     * @return the result of the query
     * @throws Exception if the query fails
     */
    protected <T> T executeQuery(DatabaseQuery<T> queryOperation) throws Exception {
        try {
            return queryOperation.execute(dynamoDb);
        } catch (Exception e) {
            log.error("Database query failed: {}", e.getMessage(), e);
            throw new RuntimeException("Database query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Functional interface for database operations (INSERT, UPDATE, DELETE)
     */
    @FunctionalInterface
    public interface DatabaseOperation {
        void execute(DynamoDbClient dynamoDb) throws Exception;
    }

    /**
     * Functional interface for database queries (SELECT)
     */
    @FunctionalInterface
    public interface DatabaseQuery<T> {
        T execute(DynamoDbClient dynamoDb) throws Exception;
    }
}

