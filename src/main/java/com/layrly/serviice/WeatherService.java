package com.layrly.serviice;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.layrly.Util.mapper;

public class WeatherService {
    private static final String WEATHER_API = "https://api.weatherapi.com/v1/current.json?key=1c4b8bdfc35c46959ff65434263103&q=%s&aqi=no";
    private static HttpClient client;

    private static HttpClient getClient() {
        if (client == null) {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }
        return client;
    }

    public static Weather getWeatherData(String zipCode) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(WEATHER_API, zipCode)))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response =
                getClient().send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode root = mapper.readTree(response.body());

        JsonNode location = root.get("location");
        JsonNode current = root.get("current");

        double tempF = current.get("temp_f").asDouble();
        double feelsLike = current.get("feelslike_f").asDouble();
        String condition = current.get("condition").get("text").asText();
        String icon = "https:" + current.get("condition").get("icon").asText();
        double wind = current.get("wind_mph").asDouble();

        String name = location.get("name").asText();
        String region = location.get("region").asText();

        // Generate description
        String breeze;
        if(wind < 10) {
            breeze = "light breeze";
        } else if(wind < 20) {
            breeze = "moderate wind";
        } else {
            breeze = "strong wind";
        }

        String description = condition + " with " + breeze;

        return new Weather(tempF, feelsLike, condition, icon, wind, name + ", " + region, description);
    }
}
