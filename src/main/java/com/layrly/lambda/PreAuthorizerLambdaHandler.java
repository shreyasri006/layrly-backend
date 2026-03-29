package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.layrly.dao.UserDAO;

import java.util.Map;
import java.util.UUID;

public class PreAuthorizerLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final UserDAO userDAO = new UserDAO();

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

            // Insert user using UserDAO
            userDAO.insertUser(UUID.fromString(userName), name, email, gender, zip);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }

        // Auto Confirm User in Cognito response
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
}
