package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.dao.WardrobeItemDAO;

import java.util.UUID;

import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class WardrobeDeleteLambdaHandler extends LambdaHandler {

    // bucket name (e.g., "layrly")
    private final WardrobeItemDAO wardrobeItemDAO = new WardrobeItemDAO();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        // Authenticated User
        String userName = getUserName(event);

        try {
            long wardrobeItemId = Long.parseLong(event.getQueryStringParameters().get("id"));

            wardrobeItemDAO.deleteWardrobeItem(wardrobeItemId, UUID.fromString(userName));

            return getApiGatewayProxyResponseEvent(200, "Wardrobe Item deleted successfully.",
                    false);
        } catch (Exception e) {
            e.printStackTrace();

            // Return error response
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }
    }
}
