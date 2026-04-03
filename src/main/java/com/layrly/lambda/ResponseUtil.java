package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {
    public static APIGatewayProxyResponseEvent getApiGatewayProxyResponseEvent(int statusCode, String message,
                                                                               boolean error) {
        APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
        errorResponse.setStatusCode(statusCode);
        if(error) {
            errorResponse.setBody("{\"error\": \"" + message + "\"}");
        } else {
            errorResponse.setBody("{\"message\": \"" + message + "\"}");
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        errorResponse.setHeaders(headers);
        return errorResponse;
    }

    public static APIGatewayProxyResponseEvent getApiGatewayProxyResponseEvent(int statusCode, String json) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody(json);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);
        return response;
    }
}
