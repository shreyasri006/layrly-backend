package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.layrly.dao.LoginHistoryDAO;

import java.util.Map;
import java.util.UUID;

import static com.layrly.Util.mapper;

public class PostAuthenticationLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final LoginHistoryDAO historyDAO = new LoginHistoryDAO();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            System.out.println("Received event: " + mapper.writeValueAsString(event));
            System.out.println("Received context: " + mapper.writeValueAsString(context));

            var userNameObj = event.get("userName");

            if(userNameObj != null) {
                historyDAO.insert(UUID.fromString(userNameObj.toString()));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }

        return event;
    }
}
