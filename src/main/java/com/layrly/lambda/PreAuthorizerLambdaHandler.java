package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;

public class PreAuthorizerLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String SECRET_ARN = "arn:aws:secretsmanager:us-east-2:710514263620:secret:rds!cluster-b08348b8-b0bc-401c-b7e3-4a7175a5f668-oYo4QI";

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {

        System.out.println("Received event: " + event);

        try {
            Map<String, Object> request = (Map<String, Object>) event.get("request");
            Map<String, String> attrs = (Map<String, String>) request.get("userAttributes");

            String userName = (String) event.getOrDefault("userName", "unknown");
            String name = attrs.getOrDefault("name", "unknown");
            String email = attrs.getOrDefault("email", "unknown");
            String gender = attrs.getOrDefault("gender", "unknown");
            String zip = attrs.getOrDefault("custom:zip", "00000");

            String password = getDbPassword();
            // System.out.println("password " + password);

            String url = "jdbc:postgresql://database-1-instance-1.c9uqeycswfv8.us-east-2.rds.amazonaws.com:5432/postgres?sslmode=require";

            try (Connection conn = DriverManager.getConnection(url, "postgres", password)) {

                String sql = "INSERT INTO users (user_name, name, email, gender, zip) VALUES (?, ?, ?, ?, ?)";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, userName);
                stmt.setString(2, name);
                stmt.setString(3, email);
                stmt.setString(4, gender);
                stmt.setString(5, zip);

                stmt.executeUpdate();
            }

        } catch (Exception e) {
            System.out.println("DB Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }

        // Cognito response
        Map<String, Object> response = (Map<String, Object>) event.get("response");

        response.put("autoConfirmUser", true);

        Map<String, Object> request = (Map<String, Object>) event.get("request");
        Map<String, String> attrs = (Map<String, String>) request.get("userAttributes");

        if (attrs.containsKey("email")) {
            response.put("autoVerifyEmail", true);
        }

        if (attrs.containsKey("phone_number")) {
            response.put("autoVerifyPhone", true);
        }

        return event;
    }

    private String getDbPassword() throws Exception {
        SecretsManagerClient client = SecretsManagerClient.create();

        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(SECRET_ARN)
                .build();

        String secretString = client.getSecretValue(request).secretString();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> secretMap = mapper.readValue(secretString, new TypeReference<Map<String, String>>() {});

        return secretMap.get("password");
    }
}