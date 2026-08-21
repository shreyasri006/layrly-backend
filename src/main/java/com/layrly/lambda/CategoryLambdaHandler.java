package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.dao.CategoryDAO;
import com.layrly.dao.DAOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.layrly.Util.mapper;
import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class CategoryLambdaHandler extends LambdaHandler {
    private static final Logger log = LoggerFactory.getLogger(CategoryLambdaHandler.class);

    private static CategoryDAO categoryDAO = DAOFactory.getDao(CategoryDAO.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            return getApiGatewayProxyResponseEvent(200,
                    mapper.writeValueAsString(categoryDAO.getAllCategories()));
        } catch (Exception e) {
            log.error("error", e);
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }
    }
}
