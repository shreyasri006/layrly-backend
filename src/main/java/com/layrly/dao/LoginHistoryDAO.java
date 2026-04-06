package com.layrly.dao;

import java.sql.PreparedStatement;
import java.util.UUID;

/**
 * Data Access Object for login_history table
 * Handles all database operations related to user login
 */
public class LoginHistoryDAO extends BaseDAO {
    public void insert(UUID userName) throws Exception {
        executeTransaction(conn -> {
            String sql = "INSERT INTO login_history (user_name) VALUES (?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, userName);

                int rowsAffected = stmt.executeUpdate();
                System.out.println("User Loginin inserted successfully. Rows affected: " + rowsAffected);
            }
        });
    }

    /**
     * Get continuous login streak count for a given user name
     * Returns the number of consecutive days the user has logged in, starting from today or yesterday.
     * If there is no login today or yesterday, returns 0.
     * Examples: 3 days streak, 2 days streak, 1 day streak, 5 days streak
     *
     * @param userName username (UUID)
     * @return count of consecutive login days; 0 if no login in last 2 days
     * @throws Exception if query fails
     */
    public long getRecentLoginsByUserName(UUID userName) throws Exception {

        return executeQuery(conn -> {
            // Calculate consecutive login days starting from today or yesterday
            String sql = """
                    WITH login_dates AS (
                        SELECT DISTINCT DATE(login_time) as login_date
                        FROM login_history
                        WHERE user_name = ?
                    ),
                    most_recent AS (
                        SELECT MAX(login_date) as last_login_date
                        FROM login_dates
                    )
                    SELECT
                        CASE
                            WHEN (SELECT last_login_date FROM most_recent) < CURRENT_DATE - INTERVAL '1 day'
                            THEN 0
                            ELSE (
                                SELECT COUNT(*)
                                FROM (
                                    SELECT
                                        login_date,
                                        ROW_NUMBER() OVER (ORDER BY login_date DESC) as row_num,
                                        (SELECT last_login_date FROM most_recent) - login_date as days_from_latest
                                    FROM login_dates
                                ) ranked_dates
                                WHERE row_num = days_from_latest + 1
                            )
                        END as count
                    """;

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
