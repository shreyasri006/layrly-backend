package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.dao.WardrobeItemDAO;
import com.layrly.domain.WardrobeAnalyzedItem;
import com.layrly.domain.WardrobeItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WardrobeListLambdaHandlerTest extends AbstractLambdaHandlerTest {

    private WardrobeListLambdaHandler handler;
    private WardrobeItemDAO mockDAO;

    @BeforeEach
    void setUp() throws Exception {
        handler = new WardrobeListLambdaHandler();
        mockDAO = mock(WardrobeItemDAO.class);

        // Use reflection to inject the mock DAO
        Field daoField = WardrobeListLambdaHandler.class.getDeclaredField("wardrobeItemDAO");
        daoField.setAccessible(true);
        daoField.set(handler, mockDAO);
    }

    @Test
    void testHandleRequest_GetAllItems_Success() throws Exception {
        APIGatewayProxyRequestEvent event = getApiGatewayProxyRequestEvent();
        event.getQueryStringParameters().put("category", "all");
        Context context = mock(Context.class);

        UUID userId = UUID.fromString("61cbb570-c061-7014-0768-39bb94515335");
        List<WardrobeItem> mockWardrobeItems = List.of(
                new WardrobeItem("1", userId, "shirt.jpg", "Shirt", "Blue", "Nike",
                        new WardrobeAnalyzedItem("1", "{\"description\":\"A blue Nike shirt\"}")),
                new WardrobeItem("2", userId, "pants.jpg", "Pants", "Black", "Levi's",
                        new WardrobeAnalyzedItem("2", "{\"description\":\"Black Levi's pants\"}"))
        );

        when(mockDAO.getWardrobeItemsByUserId(userId)).thenReturn(mockWardrobeItems);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Assert
        assertEquals(200, response.getStatusCode());
        var jsonNode = mapper.readTree(response.getBody());
        assertEquals(2, jsonNode.size());
    }

    @Test
    void testHandleRequest_GetItemsByCategory_Success() throws Exception {
        APIGatewayProxyRequestEvent event = getApiGatewayProxyRequestEvent();
        event.getQueryStringParameters().put("category", "Shirt");
        Context context = mock(Context.class);

        UUID userId = UUID.fromString("61cbb570-c061-7014-0768-39bb94515335");
        List<WardrobeItem> mockWardrobeItems = List.of(
                new WardrobeItem("1", userId, "shirt.jpg", "Shirt", "Blue", "Nike",
                        new WardrobeAnalyzedItem("1", "{\"description\":\"A blue Nike shirt\"}"))
        );

        when(mockDAO.getWardrobeItemsByUserNameAndCategory(userId, "Shirt")).thenReturn(mockWardrobeItems);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Assert
        assertEquals(200, response.getStatusCode());
        var jsonNode = mapper.readTree(response.getBody());
        assertEquals(1, jsonNode.size());
    }

    @Test
    void testHandleRequest_GetAllItemsWithAllCategory_Success() throws Exception {
        APIGatewayProxyRequestEvent event = getApiGatewayProxyRequestEvent();
        Context context = mock(Context.class);

        UUID userId = UUID.fromString("61cbb570-c061-7014-0768-39bb94515335");
        List<WardrobeItem> mockWardrobeItems = List.of(
                new WardrobeItem("1", userId, "shirt.jpg", "Shirt", "Blue", "Nike",
                        null),
                new WardrobeItem("2", userId, "pants.jpg", "Pants", "Black", "Levi's",
                        null)
        );

        when(mockDAO.getWardrobeItemsByUserId(userId)).thenReturn(mockWardrobeItems);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Assert
        assertEquals(200, response.getStatusCode());
        var jsonNode = mapper.readTree(response.getBody());
        assertEquals(2, jsonNode.size());
    }
}
