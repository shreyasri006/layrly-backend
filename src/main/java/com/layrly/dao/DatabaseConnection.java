package com.layrly.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;

/**
 * DatabaseConnection utility class for managing PostgreSQL connections
 * Handles credential retrieval from AWS Secrets Manager and connection pooling
 */
public class DatabaseConnection {

    private static final String DB_URL = "jdbc:postgresql://database-1-instance-1.c9uqeycswfv8.us-east-2.rds.amazonaws.com:5432/postgres?sslmode=require";
    private static final String DB_USER = "postgres";

    private static HikariDataSource dataSource;

    /**
     * Lazily initialize the DataSource on first use.
     * This reduces Lambda cold start time by deferring expensive initialization.
     *
     * @return initialized HikariDataSource
     * @throws Exception if initialization fails
     */
    private static synchronized HikariDataSource getDataSource() throws Exception {
        if (dataSource == null) {
            String password = getDbPassword();
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USER);
            config.setPassword(password);
            config.setMaximumPoolSize(2);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(3000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setAutoCommit(false);
            config.setConnectionTestQuery(null);
            config.setValidationTimeout(1000);

            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    /**
     * Get a new database connection from the pool
     *
     * @return Connection object
     * @throws Exception if connection fails
     */
    public static Connection getConnection() throws Exception {
        return getDataSource().getConnection();
    }

    /**
     * Retrieve database password from AWS Secrets Manager
     *
     * @return database password
     * @throws Exception if retrieval fails
     */
    private static String getDbPassword() throws Exception {
//        try (SecretsManagerClient client = SecretsManagerClient.create()) {
//            GetSecretValueRequest request = GetSecretValueRequest.builder()
//                    .secretId(SECRET_ARN)
//                    .build();
//
//            String secretString = client.getSecretValue(request).secretString();
//
//            ObjectMapper mapper = new ObjectMapper();
//            Map<String, String> secretMap = mapper.readValue(secretString, new TypeReference<>() {
//            });
//
//            return secretMap.get("password");
//        }
        return System.getenv("DB_PASSWORD");
    }

    /**
     * Close a database connection and rollback if needed (returns connection to pool)
     *
     * @param conn           Connection to close
     * @param shouldRollback whether to rollback before closing
     */
    public static void closeConnection(Connection conn, boolean shouldRollback) {
        if(conn != null) {
            try {
                if(shouldRollback) {
                    conn.rollback();
                }
            } catch (Exception e) {
                System.out.println("Rollback failed: " + e.getMessage());
            } finally {
                try {
                    conn.close(); // Return connection to the pool
                } catch (Exception e) {
                    System.out.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Close a database connection (returns connection to pool, assumes transaction was committed)
     *
     * @param conn Connection to close
     */
    public static void closeConnection(Connection conn) {
        closeConnection(conn, false);
    }
}
