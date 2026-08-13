package com.layrly.dao;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Abstract base class for all Data Access Objects
 * Provides common database operations for specific tables
 */
public abstract class BaseDAO {

    /**
     * Execute a database operation with automatic transaction management
     *
     * @param operation the database operation to execute
     * @throws Exception if the operation fails
     */
    protected void executeTransaction(DatabaseOperation operation) throws Exception {
        DynamoDbClient dynamoDb = DynamoDbClient.create();
        try {
            operation.execute(dynamoDb);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
//            if (conn != null && !conn.isClosed()) {
//                DatabaseConnection.closeConnection(conn);
//            }
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
        DynamoDbClient dynamoDb = DynamoDbClient.create();
        try {
            return queryOperation.execute(dynamoDb);
        } catch (Exception e) {
            System.out.println("Database query failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database query failed: " + e.getMessage(), e);
        } finally {
//            DatabaseConnection.closeConnection(dynamoDb);
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

