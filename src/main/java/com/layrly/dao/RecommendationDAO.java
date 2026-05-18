package com.layrly.dao;

import com.layrly.domain.User;

import java.sql.PreparedStatement;
import java.util.UUID;

/**
 * Data Access Object for recommendations table
 * Handles all database operations related to recommendations
 */
public class RecommendationDAO extends BaseDAO {
    public void insert(UUID userName, String context, String outfits, String model) throws Exception {
        executeTransaction(conn -> {
            String sql = "INSERT INTO recommendations (user_name, context, outfits, model_version) VALUES (?, ?::jsonb, ?::jsonb, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, userName);
                stmt.setString(2, context);
                stmt.setString(3, outfits);
                stmt.setString(4, model);

                int rowsAffected = stmt.executeUpdate();
                System.out.println("Recommendation inserted successfully. Rows affected: " + rowsAffected);
            }
        });
    }

    /**
     * Get LatestOutFit by username that was created in the last X hours
     *
     * @param userName username
     * @param userName username
     * @return outFits (Recommendation)
     * @throws Exception if query fails
     */
    public String getLatestOutFitByUserNameAndCreatedTime(UUID userName, int hours) throws Exception {                //2
        return executeQuery(conn -> {
            String sql = "SELECT outfits::text as outfits FROM recommendations WHERE user_name = ? AND created_at > NOW() - INTERVAL '1 hour' * ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, userName);
                stmt.setInt(2, hours);
                try (var rs = stmt.executeQuery()) {
                    if(rs.next()) {
                        return rs.getString("outfits");
                    }
                }
            }
            return null;
        });
    }

    /**
     * Get total count of recommendations for a given user name
     *
     * @param userName username
     * @return count of recommendations
     * @throws Exception if query fails
     */
    public long getTotalRecommendationsCountByUserName(UUID userName) throws Exception {                             //2
        return executeQuery(conn -> {
            String sql = "SELECT COUNT(*) as count FROM recommendations WHERE user_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, userName);
                try (var rs = stmt.executeQuery()) {
                    if(rs.next()) {
                        return rs.getLong("count");
                    }
                }
            }
            return 0L;
        });
    }


}
