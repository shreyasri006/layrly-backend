package com.layrly.lambda;

import java.util.List;
import java.util.Map;

public record Recommendation(int recommendation_id, List<Map<String, Object>> items) {
}
