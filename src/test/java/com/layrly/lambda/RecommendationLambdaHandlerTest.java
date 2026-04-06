package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.layrly.ai.ImageAnalyzer;
import com.layrly.dao.RecommendationDAO;
import com.layrly.dao.WardrobeItemDAO;
import com.layrly.domain.WardrobeAnalyzedItem;
import com.layrly.domain.WardrobeItem;
import com.layrly.serviice.Weather;
import com.layrly.serviice.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RecommendationLambdaHandlerTest extends AbstractLambdaHandlerTest {

    private RecommendationLambdaHandler handler;
    private WardrobeItemDAO wardrobeItemDAO;
    private RecommendationDAO recommendationDAO;
    private ObjectMapper mapper = new ObjectMapper();
    private ImageAnalyzer imageAnalyzer;

    @BeforeEach
    void setUp() throws Exception {
        handler = new RecommendationLambdaHandler();
        wardrobeItemDAO = mock(WardrobeItemDAO.class);
        recommendationDAO = mock(RecommendationDAO.class);
        imageAnalyzer = mock(ImageAnalyzer.class);

        // Use reflection to inject the mock DAO
        Field daoField = RecommendationLambdaHandler.class.getDeclaredField("wardrobeItemDAO");
        daoField.setAccessible(true);
        daoField.set(handler, wardrobeItemDAO);

        Field imageAnalyzerField = RecommendationLambdaHandler.class.getDeclaredField("imageAnalyzer");
        imageAnalyzerField.setAccessible(true);
        imageAnalyzerField.set(handler, imageAnalyzer);

        Field recommendationDAOField = RecommendationLambdaHandler.class.getDeclaredField("recommendationDAO");
        recommendationDAOField.setAccessible(true);
        recommendationDAOField.set(handler, recommendationDAO);
    }

    @Test
    void testHandleRequest_Success() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = getApiGatewayProxyRequestEvent();
        Context context = mock(Context.class);

        String userName = "61cbb570-c061-7014-0768-39bb94515335";
        String zipCode = "46202";

        Weather mockWeather = new Weather(70.0, 65.0, "Sunny", "sunny.png", 10.0, "New York", "Clear skies");
        List<WardrobeItem> mockWardrobeItems = List.of(
                new WardrobeItem("1", UUID.fromString(userName), "shirt.jpg", "Shirt", "Blue", "Nike",
                        new WardrobeAnalyzedItem("1", "{\"items\":[{\"fit\":\"unknown\",\"type\":\"jacket\",\"color\":\"blue\",\"layer\":\"outer\",\"style\":\"casual\",\"season\":[\"fall\",\"winter\"],\"pattern\":\"solid\",\"material\":\"synthetic\",\"occasion\":[\"casual\"],\"confidence\":\"high\",\"color_family\":\"blue\",\"formality_level\":2,\"temperature_range_f\":[32,50]}],\"description\":\"A blue puffer jacket with a hood.\"}"))
        );

        String expectedAiResponse = "[{\"recommendation_id\": 1, \"items\": [{\"apparel_id\": \"1\", \"type\": \"shirt\", \"description\": \"Blue Nike shirt\"}, {\"apparel_id\": \"1\", \"type\": \"shirt\", \"description\": \"Blue Nike shirt\"}]}]";
        List<Recommendation> expectedRecommendations = List.of(
                new Recommendation(1, List.of(Map.of("apparel_id", "1", "type", "shirt", "description", "Blue Nike shirt")))
        );

        when(wardrobeItemDAO.getWardrobeItemsByUserId(UUID.fromString(userName))).thenReturn(mockWardrobeItems);

        try (MockedStatic<WeatherService> mockedWeatherService = mockStatic(WeatherService.class);
             MockedStatic<ImageAnalyzer> mockedImageAnalyzer = mockStatic(ImageAnalyzer.class)) {

            mockedWeatherService.when(() -> WeatherService.getWeatherData(zipCode)).thenReturn(mockWeather);
            mockedImageAnalyzer.when(() -> imageAnalyzer.generateRecommendation(anyString())).thenReturn(expectedAiResponse);

            // Act
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

            // Assert
            assertEquals(200, response.getStatusCode());

            RecommendationResponse responseBody = mapper.readValue(response.getBody(), RecommendationResponse.class);
            assertEquals(mockWeather, responseBody.weather());
            assertEquals(expectedRecommendations.size(), responseBody.recommendations().size());

            verify(wardrobeItemDAO).getWardrobeItemsByUserId(UUID.fromString(userName));

            System.out.println("Response Body: " + response.getBody());
        }
    }

    @Test
    void testHandleRequest_With_Cache_Success() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = getApiGatewayProxyRequestEvent();
        Context context = mock(Context.class);

        String userName = "61cbb570-c061-7014-0768-39bb94515335";
        String zipCode = "46202";

        when(recommendationDAO.getLatestOutFitByUserNameAndCreatedTime(UUID.fromString(userName), 1)).thenReturn("{\"weather\":{\"temperature\":70.0,\"feels_like\":65.0,\"description\":\"Sunny\",\"icon\":\"sunny.png\",\"wind_speed\":10.0,\"city_name\":\"New York\",\"detailed_description\":\"Clear skies\"},\"recommendations\":[{\"recommendation_id\":1,\"items\":[{\"apparel_id\":\"1\",\"type\":\"shirt\",\"description\":\"Blue Nike shirt\"}]}]}");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Assert
        assertEquals(200, response.getStatusCode());

        verify(recommendationDAO).getLatestOutFitByUserNameAndCreatedTime(UUID.fromString(userName), 1);

        System.out.println("Response Body: " + response.getBody());
    }
}
