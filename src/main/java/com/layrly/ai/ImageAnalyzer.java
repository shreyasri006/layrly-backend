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

import static com.layrly.ai.Prompts.RECOMMENDATION_PROMPT;

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
            String requestBody = mapper.writeValueAsString(Map.of("model", "qwen/qwen3.6-27b",
                    "temperature", 0.2,
                    "messages", List.of(Map.of("role", "user",
                            "content", List.of(Map.of("type", "text",
                                    "text", textPrompt), Map.of("type", "image_url",
                                    "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)))))));

            // Create HTTP request
            HttpRequest request = getHttpRequest(requestBody);

            // Send request and get response
            HttpResponse<String> response = getClient().send(request, HttpResponse.BodyHandlers.ofString());


            if (response.statusCode() == 200) {
                // Parse and print the response
                JsonNode jsonResponse = mapper.readTree(response.body());
                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

                if (content.contains("</think>")) {
                    content = content.substring(content.indexOf("</think>") + 8);
                    content = content.replace(System.lineSeparator(), "");
                }

                if (content.contains("```")) {
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

            if (response.statusCode() == 200) {
                // Parse and print the response
                JsonNode jsonResponse = mapper.readTree(response.body());
                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

                if (content.contains("</think>")) {
                    content = content.substring(content.indexOf("</think>") + 8);
                    content = content.replace(System.lineSeparator(), "");
                }

                if (content.contains("```")) {
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
        if (apiKey == null || apiKey.isEmpty()) {
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

    public static void main(String[] args) {
        ImageAnalyzer analyzer = new ImageAnalyzer();
        try {
//            String base64Image = "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";
//            String metadata = analyzer.extractMetadata(IMAGE_META_DATA_EXTRACT_PROMPT, base64Image);
//            System.out.println("Extracted Metadata: " + metadata);


            String recommendation = analyzer.generateRecommendation(RECOMMENDATION_PROMPT);
            System.out.println("recommendation: " + recommendation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
