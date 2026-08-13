package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.dao.DAOFactory;
import com.layrly.dao.WardrobeItemDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class WardrobeDeleteLambdaHandler extends LambdaHandler {
    private static final Logger log = LoggerFactory.getLogger(WardrobeDeleteLambdaHandler.class);

    // bucket name (e.g., "layrly")
    private static WardrobeItemDAO wardrobeItemDAO = DAOFactory.getDao(WardrobeItemDAO.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        // Authenticated User
        String userName = getUserName(event);

        try {
            String wardrobeItemId = event.getQueryStringParameters().get("id");

            wardrobeItemDAO.deleteWardrobeItem(wardrobeItemId, UUID.fromString(userName));

            return getApiGatewayProxyResponseEvent(200, "Wardrobe Item deleted successfully.",
                    false);
        } catch (Exception e) {
            log.error("Error", e);

            // Return error response
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }
    }
}
