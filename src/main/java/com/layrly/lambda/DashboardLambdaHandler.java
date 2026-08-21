package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.dao.CategoryDAO;
import com.layrly.dao.DAOFactory;
import com.layrly.dao.LoginHistoryDAO;
import com.layrly.dao.RecommendationDAO;
import com.layrly.dao.WardrobeItemDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static com.layrly.Util.mapper;
import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class DashboardLambdaHandler extends LambdaHandler {
    private static final Logger log = LoggerFactory.getLogger(DashboardLambdaHandler.class);

    private static WardrobeItemDAO wardrobeItemDAO = DAOFactory.getDao(WardrobeItemDAO.class);
    private static RecommendationDAO recommendationDAO = DAOFactory.getDao(RecommendationDAO.class);
    private static LoginHistoryDAO historyDAO = DAOFactory.getDao(LoginHistoryDAO.class);
    private static CategoryDAO categoryDAO = DAOFactory.getDao(CategoryDAO.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            // Authenticated User
            String userName = getUserName(event);

            long start = System.currentTimeMillis();
            CompletableFuture<Integer> rec = CompletableFuture.supplyAsync(() -> sneaky(() -> recommendationDAO.getTotalRecommendationsCountByUserName(userName)));
            CompletableFuture<Integer> ward = CompletableFuture.supplyAsync(() -> sneaky(() -> wardrobeItemDAO.getApparelCountByUserName(userName)));
            CompletableFuture<Integer> login = CompletableFuture.supplyAsync(() -> sneaky(() -> historyDAO.getRecentLoginsByUserName(userName)));
            CompletableFuture<Integer> cat = CompletableFuture.supplyAsync(() -> sneaky(() -> categoryDAO.getTotalCategories()));
            log.info("Initiate fetch time: {}ms", System.currentTimeMillis() - start);

            Map<String, Integer> stats = Map.of(
                    "recommendationsCount", rec.get(),
                    "wardrobeItemCount", ward.get(),
                    "recentLoginCount", login.get(),
                    "categoriesCount", cat.get()
            );
            log.info("Total fetch time: {}ms", System.currentTimeMillis() - start);

            return getApiGatewayProxyResponseEvent(200, mapper.writeValueAsString(stats));
        } catch (Exception e) {
            e.printStackTrace();
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }
    }

    private static <T> T sneaky(Callable<T> c) {
        try {
            return c.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
