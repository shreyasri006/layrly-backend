package com.layrly.domain;

import java.util.UUID;

public record User(UUID userName, String name, String email, String gender, String zip) {
}
