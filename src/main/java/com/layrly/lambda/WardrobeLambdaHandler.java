package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class WardrobeLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // S3_BUCKET_NAME should be just the bucket name (e.g., "layrly"), not the full S3 path
    // The S3 path/key is specified separately in the putRequest
    private static final String S3_BUCKET_NAME = "layrly";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        System.out.println("Received event: " + event);

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> requestBody = mapper.readValue(event.getBody(), Map.class);

            System.out.println("Received requestBody Keys: " + requestBody.keySet());

            String imageBase64 = (String) requestBody.get("image");
            String fileName = (String) requestBody.get("fileName");
            // Add other parameters as needed
            String category = (String) requestBody.get("category");
            String color = (String) requestBody.get("color");
            String brand = (String) requestBody.get("brand");

            // Print request parameters
            System.out.println("File Name: " + fileName);
            System.out.println("Category: " + category);
            System.out.println("Color: " + color);
            System.out.println("Brand: " + brand);

            // Decode base64 image
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

            // Upload to S3 in the images folder
            String s3Key = "images/" + fileName;  // Store in images folder
            
            try (S3Client s3Client = S3Client.builder()
                    .region(Region.US_EAST_2)
                    .build()) {
                
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(S3_BUCKET_NAME)
                        .key(s3Key)
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(imageBytes));
                System.out.println("Image uploaded to S3: " + s3Key);
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();

            // Return error response
            APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
            errorResponse.setStatusCode(500);
            errorResponse.setBody("{\"error\": \"" + e.getMessage() + "\"}");
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            errorResponse.setHeaders(headers);
            return errorResponse;
        }

        // Success response
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody("{\"message\": \"Image uploaded successfully\"}");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);
        return response;
    }
}
