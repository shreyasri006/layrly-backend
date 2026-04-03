package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

public abstract class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    protected static String getUserName(APIGatewayProxyRequestEvent event) {
        return String.valueOf(((Map<String, Object>) ((Map<String, Object>)
                event.getRequestContext().getAuthorizer().get("jwt")).get("claims")).get("cognito:username"));
    }

    protected static String getUserZip(APIGatewayProxyRequestEvent event) {
        return String.valueOf(((Map<String, Object>) ((Map<String, Object>)
                event.getRequestContext().getAuthorizer().get("jwt")).get("claims")).get("custom:zip"));
    }
}
