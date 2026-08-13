package com.layrly.domain;

public record Category(int id, String name, int displayOrder) {
    public int getDisplayOrder() {
        return displayOrder;
    }
}
