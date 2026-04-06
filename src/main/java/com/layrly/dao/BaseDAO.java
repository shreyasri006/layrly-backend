package com.layrly.dao;

import java.sql.Connection;

/**
 * Abstract base class for all Data Access Objects
 * Provides common database operations for specific tables
 */
public abstract class BaseDAO {

    /**
     * Execute a database operation with automatic transaction management
     * @param operation the database operation to execute
     * @throws Exception if the operation fails
     */
    protected void executeTransaction(DatabaseOperation operation) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            operation.execute(conn);
            conn.commit();
        } catch (Exception e) {
            DatabaseConnection.closeConnection(conn, true);
            e.printStackTrace();
            throw new RuntimeException("Database operation failed: " + e.getMessage(), e);
        } finally {
            if (conn != null && !conn.isClosed()) {
                DatabaseConnection.closeConnection(conn);
            }
        }
    }

    /**
     * Execute a database query with automatic resource management
     * @param queryOperation the database query to execute
     * @return the result of the query
     * @throws Exception if the query fails
     */
    protected <T> T executeQuery(DatabaseQuery<T> queryOperation) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return queryOperation.execute(conn);
        } catch (Exception e) {
            System.out.println("Database query failed: " + e.getMessage());
            throw new RuntimeException("Database query failed: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    /**
     * Functional interface for database operations (INSERT, UPDATE, DELETE)
     */
    @FunctionalInterface
    public interface DatabaseOperation {
        void execute(Connection conn) throws Exception;
    }

    /**
     * Functional interface for database queries (SELECT)
     */
    @FunctionalInterface
    public interface DatabaseQuery<T> {
        T execute(Connection conn) throws Exception;
    }
}

