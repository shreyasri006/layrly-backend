package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.dao.DAOFactory;
import com.layrly.dao.WardrobeItemDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

import static com.layrly.Util.mapper;
import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class WardrobeEditLambdaHandler extends LambdaHandler {
    private static final Logger log = LoggerFactory.getLogger(WardrobeEditLambdaHandler.class);

    // bucket name (e.g., "layrly")
    private static WardrobeItemDAO wardrobeItemDAO = DAOFactory.getDao(WardrobeItemDAO.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        // Authenticated User
        String userName = getUserName(event);

        try {
            String wardrobeItemId = event.getQueryStringParameters().get("id");

            Map<String, Object> requestBody = mapper.readValue(event.getBody(), Map.class);

            log.info("Received requestBody Keys: {}", requestBody.keySet());

            String category = (String) requestBody.get("category");
            String color = (String) requestBody.get("color");
            String brand = (String) requestBody.get("brand");

            wardrobeItemDAO.updateWardrobeItem(wardrobeItemId, category, color, brand, UUID.fromString(userName));

            return getApiGatewayProxyResponseEvent(200, "Wardrobe Item updated successfully.",
                    false);
        } catch (Exception e) {
            log.error("Error", e);

            // Return error response
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }
    }
}
