package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.ai.ImageAnalyzer;
import com.layrly.dao.WardrobeItemDAO;
import com.layrly.domain.WardrobeAnalyzedItem;
import com.layrly.domain.WardrobeItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WardrobeLambdaHandlerTest extends AbstractLambdaHandlerTest {

    private WardrobeLambdaHandler handler;
    private WardrobeItemDAO mockDAO;
    private ImageAnalyzer imageAnalyzer;

    @BeforeEach
    void setUp() throws Exception {
        handler = new WardrobeLambdaHandler();
        mockDAO = mock(WardrobeItemDAO.class);
        imageAnalyzer = mock(ImageAnalyzer.class);

        // Use reflection to inject the mock DAO
        Field daoField = WardrobeLambdaHandler.class.getDeclaredField("wardrobeItemDAO");
        daoField.setAccessible(true);
        daoField.set(handler, mockDAO);

        daoField = WardrobeLambdaHandler.class.getDeclaredField("imageAnalyzer");
        daoField.setAccessible(true);
        daoField.set(handler, imageAnalyzer);
    }

    @Test
    void testHandleRequest_Success() throws Exception {
        APIGatewayProxyRequestEvent event = getApiGatewayProxyRequestEvent();
        Context context = mock(Context.class);

        String imageBase64 = Base64.getEncoder().encodeToString("fake image data".getBytes());
        String fileName = "test.jpg";
        String category = "Shirt";
        String color = "Blue";
        String brand = "Nike";

        Map<String, Object> requestBody = Map.of(
                "image", imageBase64,
                "fileName", fileName,
                "category", category,
                "color", color,
                "brand", brand
        );

        event.setBody(mapper.writeValueAsString(requestBody));

        String expectedMetadata = "Extracted metadata";
        WardrobeAnalyzedItem expectedAnalyzedItem = new WardrobeAnalyzedItem(null, expectedMetadata);

        try (MockedStatic<ImageAnalyzer> mockedImageAnalyzer = mockStatic(ImageAnalyzer.class);
             MockedStatic<S3Client> mockedS3Client = mockStatic(S3Client.class)) {

            S3Client mockS3 = mock(S3Client.class);
            S3ClientBuilder mockBuilder = mock(S3ClientBuilder.class);
            mockedS3Client.when(S3Client::builder).thenReturn(mockBuilder);

            when(mockBuilder.region(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockS3);

            mockedImageAnalyzer.when(() -> imageAnalyzer.extractMetadata(anyString(), eq(imageBase64)))
                    .thenReturn(expectedMetadata);

            // Act
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

            // Assert
            assertEquals(200, response.getStatusCode());
            assertEquals("{\"message\": \"Image uploaded successfully\"}", response.getBody());

            verify(mockDAO).insertWardrobeItem(any(WardrobeItem.class));
        }
    }
}
