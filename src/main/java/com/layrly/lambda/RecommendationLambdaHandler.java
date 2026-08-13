package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.serviice.RecommendationService;
import com.layrly.serviice.Response;

import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class RecommendationLambdaHandler extends LambdaHandler {
    private final RecommendationService service = new RecommendationService();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        // Authenticated User
        String userName = getUserName(event);
        String zipCode = getUserZip(event);

        if (userName == null || zipCode == null) {
            return getApiGatewayProxyResponseEvent(401, "Unauthorized", true);
        }

        // check DB if we have already created Recommendation in the last 1 hour
        Response response = service.getRecommendations(userName, zipCode);

        if (response.statusCode() == 200) {
            return getApiGatewayProxyResponseEvent(200, response.body());
        } else {
            return getApiGatewayProxyResponseEvent(response.statusCode(), response.body(), true);
        }
    }
}
