package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.layrly.ai.ImageAnalyzer;
import com.layrly.dao.WardrobeItemDAO;
import com.layrly.domain.WardrobeAnalyzedItem;
import com.layrly.domain.WardrobeItem;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static com.layrly.Util.mapper;
import static com.layrly.ai.Prompts.IMAGE_META_DATA_EXTRACT_PROMPT;
import static com.layrly.lambda.ResponseUtil.getApiGatewayProxyResponseEvent;

public class WardrobeLambdaHandler extends LambdaHandler {

    // bucket name (e.g., "layrly")
    private static final String S3_BUCKET_NAME = "layrly";
    private final WardrobeItemDAO wardrobeItemDAO = new WardrobeItemDAO();
    private final ImageAnalyzer imageAnalyzer = new ImageAnalyzer();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        // Authenticated User
        String userName = getUserName(event);

        try {
            Map<String, Object> requestBody = mapper.readValue(event.getBody(), Map.class);

            System.out.println("Received requestBody Keys: " + requestBody.keySet());

            String imageBase64 = (String) requestBody.get("image");
            String fileName = UUID.randomUUID() + "/" + requestBody.get("fileName");
            // Add other parameters as needed
            String category = (String) requestBody.get("category");
            String color = (String) requestBody.get("color");
            String brand = (String) requestBody.get("brand");

            // Print request parameters
            System.out.println("File Name: " + fileName + ", Category: " + category + ", Color: " + color +
                    ", Brand: " + brand);

            // Upload to S3 in the images folder
            String s3Key = "images/" + fileName;  // Store in images folder
            upload(s3Key, imageBase64);

            String metadata = imageAnalyzer.extractMetadata(IMAGE_META_DATA_EXTRACT_PROMPT, imageBase64);
            WardrobeAnalyzedItem analyzedItem = new WardrobeAnalyzedItem(null, metadata);

            // insert into DB
            WardrobeItem item = new WardrobeItem(
                    null, UUID.fromString(userName), s3Key, category, color,
                    brand, analyzedItem);
            wardrobeItemDAO.insertWardrobeItem(item);
        } catch (Exception e) {
            e.printStackTrace();

            // Return error response
            return getApiGatewayProxyResponseEvent(500, e.getMessage(), true);
        }

        // Success response
        return getApiGatewayProxyResponseEvent(200, "Image uploaded successfully", false);
    }

    private static void upload(String s3Key, String imageBase64) {
        try (S3Client s3Client = S3Client.builder()
                .region(Region.US_EAST_2)
                .build()) {

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key("ui/" + s3Key) // ui/ prefix is set in cloudfront.
                    .build();

            // Decode base64 image
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

            s3Client.putObject(putRequest, RequestBody.fromBytes(imageBytes));
            System.out.println("Image uploaded to S3: " + s3Key);
        }
    }
}
