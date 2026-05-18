package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.dao.WardrobeItemDAO;
import com.layrly.domain.WardrobeItem;

import java.util.List;
import java.util.UUID;

import static com.layrly.Util.CLOUDFRONT_DOMAIN;
import static com.layrly.Util.mapper;
import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class WardrobeListLambdaHandler extends LambdaHandler {

    private final WardrobeItemDAO wardrobeItemDAO = new WardrobeItemDAO();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            // Authenticated User
            String userName = getUserName(event);
            String category = event.getQueryStringParameters().get("category");

            List<WardrobeItem> response;
            if(category == null || "all".equalsIgnoreCase(category)) {
                response = wardrobeItemDAO.getWardrobeItemsByUserId(UUID.fromString(userName));
            } else {
                response = wardrobeItemDAO.getWardrobeItemsByUserNameAndCategory(UUID.fromString(userName), category);
            }

            response = populateImageUrls(response);

            return getApiGatewayProxyResponseEvent(200, mapper.writeValueAsString(response));
        } catch (Exception e) {
            e.printStackTrace();
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }
    }

    private List<WardrobeItem> populateImageUrls(List<WardrobeItem> wardrobeItems) {
        return wardrobeItems.stream().map(item -> new WardrobeItem(item.id(), item.userName(),
                getS3ImageUrl(item.fileName()), item.category(), item.color(),
                item.brand(), null)).toList();
    }

    private String getS3ImageUrl(String fileName) {
        return String.format("%s/%s", CLOUDFRONT_DOMAIN, fileName);
    }
}
