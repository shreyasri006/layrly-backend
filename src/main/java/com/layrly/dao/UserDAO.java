package com.layrly.dao;

import com.layrly.domain.User;

import java.sql.PreparedStatement;
import java.util.UUID;

/**
 * Data Access Object for Users table
 * Handles all database operations related to users
 */
public class UserDAO extends BaseDAO {

    /**
     * Insert a new user into the database
     *
     * @param userName unique username
     * @param name     full name
     * @param email    email address
     * @param gender   gender
     * @param zip      zip code
     * @throws Exception if insertion fails
     */
    public void insertUser(UUID userName, String name, String email, String gender, String zip) throws Exception {
        executeTransaction(conn -> {
            String sql = "INSERT INTO users (user_name, name, email, gender, zip) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, userName);
                stmt.setString(2, name);
                stmt.setString(3, email);
                stmt.setString(4, gender);
                stmt.setString(5, zip);

                int rowsAffected = stmt.executeUpdate();
                System.out.println("User inserted successfully. Rows affected: " + rowsAffected);
            }
        });
    }

    /**
     * Check if a user exists by username
     *
     * @param userName username to check
     * @return true if user exists, false otherwise
     * @throws Exception if query fails
     */
    public boolean userExists(UUID userName) throws Exception {
        return executeQuery(conn -> {
            String sql = "SELECT 1 FROM users WHERE user_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, userName);
                try (var rs = stmt.executeQuery()) {
                    if(rs.next()) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    /**
     * Get user by username
     *
     * @param userName username
     * @return User object or null if not found
     * @throws Exception if query fails
     */
    public User getUserByUsername(UUID userName) throws Exception {
        return executeQuery(conn -> {
            String sql = "SELECT user_name, name, email, gender, zip FROM users WHERE user_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, userName);
                try (var rs = stmt.executeQuery()) {
                    if(rs.next()) {
                        return new User(
                                rs.getObject("user_name", UUID.class),
                                rs.getString("name"),
                                rs.getString("email"),
                                rs.getString("gender"),
                                rs.getString("zip")
                        );
                    }
                }
            }
            return null;
        });
    }
}
