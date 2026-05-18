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
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class ImageAnalyzer {
    private static final String apiKey = System.getenv("GROQ_API_KEY");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static HttpClient client;

    private static HttpClient getClient() {
        if (client == null) {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(25))
                    .build();
        }
        return client;
    }

    public String extractMetadata(String textPrompt, String base64Image) {
        checkAPIKey();

        try {
            // Create request body
            String requestBody = mapper.writeValueAsString(Map.of("model", "meta-llama/llama-4-scout-17b-16e-instruct",
                    "temperature", 0.2,
                    "messages", List.of(Map.of("role", "user",
                            "content", List.of(Map.of("type", "text",
                                    "text", textPrompt), Map.of("type", "image_url",
                                    "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)))))));

            // Create HTTP request
            HttpRequest request = getHttpRequest(requestBody);

            // Send request and get response
            HttpResponse<String> response = getClient().send(request, HttpResponse.BodyHandlers.ofString());


            if(response.statusCode() == 200) {
                // Parse and print the response
                JsonNode jsonResponse = mapper.readTree(response.body());
                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

                if(content.contains("```")) {
                    content = content.substring(content.indexOf("```") + 3, content.lastIndexOf("```"));
                    content = content.replace(System.lineSeparator(), "");
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

    public String generateRecommendation(String textPrompt) {
        checkAPIKey();

        try {
            // Create request body (llama-3.3-70b-versatile or llama-3.1-8b-instant)
            String requestBody = mapper.writeValueAsString(Map.of("model", "llama-3.3-70b-versatile",
                    "temperature", 0.2,
                    "messages", List.of(Map.of("role", "user",
                            "content", List.of(Map.of("type", "text", "text", textPrompt))))));

            // Create HTTP request
            HttpRequest request = getHttpRequest(requestBody);

            // Send request and get response
            HttpResponse<String> response = getClient().send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 200) {
                // Parse and print the response
                JsonNode jsonResponse = mapper.readTree(response.body());
                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

                if(content.contains("```")) {
                    content = content.substring(content.indexOf("```") + 3, content.lastIndexOf("```"));
                    content = content.replace("json[", "[");
                    content = content.replace(System.lineSeparator(), "");
                }
                return content;
            } else {
                System.err.println("API Error: " + response.statusCode());
                System.err.println("Response: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Failed to generate Recommendations");
    }

    private static void checkAPIKey() {
        if(apiKey == null || apiKey.isEmpty()) {
            System.err.println("GROQ_API_KEY environment variable not set");
            throw new RuntimeException("AI API_KEY environment variable not set");
        }
    }

    // encode image to Base64
    private static String encodeImage(String imagePath) throws IOException {
        File file = new File(imagePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    private static HttpRequest getHttpRequest(String requestBody) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(5))
                .build();
        return request;
    }
}
