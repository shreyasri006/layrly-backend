package com.layrly.domain;

import java.util.UUID;

public record WardrobeItem(String id, UUID userName, String fileName, String category, String color,
                           String brand, WardrobeAnalyzedItem analyzedItem) {
}
