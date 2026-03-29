package com.layrly.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class ImageAnalyzer {
    private static final String apiKey = System.getenv("GROQ_API_KEY");
    private static final ObjectMapper mapper = new ObjectMapper();


    // Function to encode the image
    public static String encodeImage(String imagePath) throws IOException {
        File file = new File(imagePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    public static String extractMetadata(String textPrompt, String base64Image) {
        if(apiKey == null || apiKey.isEmpty()) {
            System.err.println("GROQ_API_KEY environment variable not set");
            throw new RuntimeException("AI API_KEY environment variable not set");
        }

        try {
            // Create HTTP client
            HttpResponse<String> response;
            try (HttpClient client = HttpClient.newHttpClient()) {

                // Create request body
                String requestBody = mapper.writeValueAsString(Map.of(
                        "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                        "temperature", 0.2,
                        "messages", List.of(Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of("type", "text", "text", textPrompt),
                                        Map.of("type", "image_url", "image_url", Map.of(
                                                "url", "data:image/jpeg;base64," + base64Image
                                        ))
                                )
                        ))
                ));

                // Create HTTP request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                // Send request and get response
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if(response.statusCode() == 200) {
                // Parse and print the response
                JsonNode jsonResponse = mapper.readTree(response.body());
                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

                if(content.contains("```")) {
                    content = content.substring(content.indexOf("```") + 3, content.lastIndexOf("```"));
                }
                // JsonNode parsedContent = mapper.readTree(content);

                return content;
            } else {
                System.err.println("API Error: " + response.statusCode());
                System.err.println("Response: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Failed to extract metadata from image");
    }
}
