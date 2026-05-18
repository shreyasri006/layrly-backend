package com.layrly.serviice;

public record Weather(double tempF, double feelsLike, String condition, String icon, double wind, String location,
                      String description) {
}
