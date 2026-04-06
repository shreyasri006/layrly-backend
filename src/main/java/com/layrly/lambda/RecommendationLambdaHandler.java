package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.layrly.ai.ImageAnalyzer;
import com.layrly.dao.RecommendationDAO;
import com.layrly.dao.WardrobeItemDAO;
import com.layrly.domain.WardrobeItem;
import com.layrly.serviice.Weather;
import com.layrly.serviice.WeatherService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.layrly.Util.CLOUDFRONT_DOMAIN;
import static com.layrly.Util.mapper;
import static com.layrly.ai.Prompts.RECOMMENDATION_PROMPT;
import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class RecommendationLambdaHandler extends LambdaHandler {
    private final WardrobeItemDAO wardrobeItemDAO = new WardrobeItemDAO();
    private final RecommendationDAO recommendationDAO = new RecommendationDAO();
    private final ImageAnalyzer imageAnalyzer = new ImageAnalyzer();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        // Authenticated User
        String userName = getUserName(event);
        String zipCode = getUserZip(event);

        if(userName == null || zipCode == null) {
            return getApiGatewayProxyResponseEvent(401, "Unauthorized", true);
        }

        try {
            // check DB if we have already created Recommendation in the last 1 hour
            String responseString = recommendationDAO.getLatestOutFitByUserNameAndCreatedTime(UUID.fromString(userName), 1);
            if(responseString != null) {
                System.out.println("Returning cached recommendation");
                return getApiGatewayProxyResponseEvent(200, responseString);
            }

            Weather weather = WeatherService.getWeatherData(zipCode);

            List<WardrobeItem> wardrobeItems = wardrobeItemDAO.getWardrobeItemsByUserId(UUID.fromString(userName));
            List<JsonNode> apparelItems = getApparelItems(wardrobeItems);

            if(apparelItems == null) {
                return getApiGatewayProxyResponseEvent(400,
                        "No wardrobe items found for user: " + userName, true);
            }

            String prompt = getRecommendationPrompt(apparelItems, weather);
            System.out.println(prompt);

            String aiResponse = imageAnalyzer.generateRecommendation(prompt);
            List<Recommendation> recommendations = mapper.readValue(aiResponse, new TypeReference<>() {
            });

            // populate image urls
            populateImageUrls(wardrobeItems, recommendations);
            var filteredRecommendations = deleteDuplicateApparelIds(recommendations);

            RecommendationResponse response = new RecommendationResponse(weather, filteredRecommendations);

            responseString = mapper.writeValueAsString(response);
            // save the recommendation to DB
            recommendationDAO.insert(UUID.fromString(userName), mapper.writeValueAsString(Map.of("prompt", prompt)),
                    responseString, "groq");

            return getApiGatewayProxyResponseEvent(200, responseString);
        } catch (Exception e) {
            e.printStackTrace();

            // Return error response
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }
    }

    private static String getRecommendationPrompt(List<JsonNode> apparelItems, Weather weather) throws JsonProcessingException {
        String wardrobeJson = mapper.writeValueAsString(apparelItems);
        String weatherJson = mapper.writeValueAsString(weather);
        String prompt = RECOMMENDATION_PROMPT.replace("{{WARDROBE_JSON}}", wardrobeJson)
                .replace("{{WEATHER_JSON}}", weatherJson);
        return prompt;
    }

    private void populateImageUrls(List<WardrobeItem> wardrobeItems, List<Recommendation> recommendations) {
        for(Recommendation recommendation : recommendations) {
            for(var item : recommendation.items()) {
                String apparelId = (String) item.get("apparel_id");
                WardrobeItem wi = wardrobeItems.stream().filter(i -> i.id().equals(apparelId)).findFirst().orElse(null);

                if(wi != null) {
                    item.put("image_url", getS3ImageUrl(wi.fileName()));
                }
            }
        }
    }

    private List<Recommendation> deleteDuplicateApparelIds(List<Recommendation> recommendations) {
        List<Recommendation> filteredRecommendations = new ArrayList<>();

        for(Recommendation recommendation : recommendations) {
            Set<String> apparelIds = new HashSet<>();
            List<Map<String, Object>> filteredItems = new ArrayList<>();
            for(var item : recommendation.items()) {
                String apparelId = (String) item.get("apparel_id");
                if(!apparelIds.contains(apparelId)) {
                    apparelIds.add(apparelId);
                    filteredItems.add(item);
                }
            }

            filteredRecommendations.add(new Recommendation(recommendation.recommendation_id(), filteredItems));
        }

        return filteredRecommendations;
    }

    private String getS3ImageUrl(String fileName) {
        return String.format("%s/%s", CLOUDFRONT_DOMAIN, fileName);
    }

    private List<JsonNode> getApparelItems(List<WardrobeItem> wardrobeItems) throws Exception {
        if(wardrobeItems == null || wardrobeItems.isEmpty()) {
            return null;
        }

        List<JsonNode> apparelItems = new ArrayList<>();
        for(WardrobeItem item : wardrobeItems) {
            var node = mapper.readValue(item.analyzedItem().aiDescription(), ObjectNode.class);
            node.put("apparel_id", item.id());
            apparelItems.add(node);
        }

        return apparelItems;
    }
}
