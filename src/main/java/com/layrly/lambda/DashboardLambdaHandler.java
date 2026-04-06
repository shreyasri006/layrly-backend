package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.dao.CategoryDAO;
import com.layrly.dao.LoginHistoryDAO;
import com.layrly.dao.RecommendationDAO;
import com.layrly.dao.WardrobeItemDAO;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.layrly.Util.mapper;
import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class DashboardLambdaHandler extends LambdaHandler {

    private final WardrobeItemDAO wardrobeItemDAO = new WardrobeItemDAO();
    private final RecommendationDAO recommendationDAO = new RecommendationDAO();
    private final LoginHistoryDAO historyDAO = new LoginHistoryDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            // Authenticated User
            UUID userName = UUID.fromString(getUserName(event));

            Map<String, Long> stats = new HashMap<>();
            stats.put("recommendationsCount", recommendationDAO.getTotalRecommendationsCountByUserName(userName));
            stats.put("wardrobeItemCount", wardrobeItemDAO.getApparelCountByUserName(userName));
            stats.put("recentLoginCount", historyDAO.getRecentLoginsByUserName(userName));
            stats.put("categoriesCount", categoryDAO.getTotalCategories());

            return getApiGatewayProxyResponseEvent(200, mapper.writeValueAsString(stats));
        } catch (Exception e) {
            e.printStackTrace();
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }
    }
}
