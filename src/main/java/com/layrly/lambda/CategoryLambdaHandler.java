package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.dao.CategoryDAO;

import static com.layrly.Util.mapper;
import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class CategoryLambdaHandler extends LambdaHandler {
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            return getApiGatewayProxyResponseEvent(200,
                    mapper.writeValueAsString(categoryDAO.getAllCategories()));
        } catch (Exception e) {
            e.printStackTrace();
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }
    }
}
