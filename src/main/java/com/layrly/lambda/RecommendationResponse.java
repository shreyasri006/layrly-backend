package com.layrly.lambda;

import com.layrly.serviice.Weather;

import java.util.List;

public record RecommendationResponse(Weather weather, List<Recommendation> recommendations) {
}

