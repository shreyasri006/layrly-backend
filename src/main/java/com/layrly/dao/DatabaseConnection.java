package com.layrly.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

/**
 * DatabaseConnection utility class for managing PostgreSQL connections
 * Handles credential retrieval from AWS Secrets Manager
 */
public class DatabaseConnection {

    private static final String DB_URL = "jdbc:postgresql://database-1-instance-1.c9uqeycswfv8.us-east-2.rds.amazonaws.com:5432/postgres?sslmode=require";
    private static final String DB_USER = "postgres";
    private static final String SECRET_ARN = "arn:aws:secretsmanager:us-east-2:710514263620:secret:rds!cluster-b08348b8-b0bc-401c-b7e3-4a7175a5f668-oYo4QI";

    /**
     * Get a new database connection
     *
     * @return Connection object
     * @throws Exception if connection fails
     */
    public static Connection getConnection() throws Exception {
        String password = getDbPassword();
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, password);
        conn.setAutoCommit(false);
        return conn;
    }

    /**
     * Retrieve database password from AWS Secrets Manager
     *
     * @return database password
     * @throws Exception if retrieval fails
     */
    private static String getDbPassword() throws Exception {
        try (SecretsManagerClient client = SecretsManagerClient.create()) {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(SECRET_ARN)
                    .build();

            String secretString = client.getSecretValue(request).secretString();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> secretMap = mapper.readValue(secretString, new TypeReference<>() {
            });

            return secretMap.get("password");
        }
    }

    /**
     * Close a database connection and rollback if needed
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
                    if(!conn.isClosed()) {
                        conn.close();
                    }
                } catch (Exception e) {
                    System.out.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Close a database connection (assumes transaction was committed)
     *
     * @param conn Connection to close
     */
    public static void closeConnection(Connection conn) {
        closeConnection(conn, false);
    }
}

