package com.layrly.dao;

/**
 * Data Access Object contract for recommendations table.
 */
public interface RecommendationDAO {
    void insert(String userName, String context, String outfits, String model) throws Exception;

    String getLatestOutFitByUserNameAndCreatedTime(String userName, int hours) throws Exception;

    int getTotalRecommendationsCountByUserName(String userName) throws Exception;
}
