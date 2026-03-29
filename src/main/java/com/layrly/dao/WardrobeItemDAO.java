package com.layrly.dao;

import com.layrly.domain.WardrobeItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for Wardrobe Items table
 */
public class WardrobeItemDAO extends BaseDAO {

    /**
     * Insert a new wardrobe item
     */
    public void insertWardrobeItem(WardrobeItem item) throws Exception {
        executeTransaction(conn -> {
            Long itemId = insertWardrobeItem(item, conn);
            insertWardrobeAnalyzedItem(itemId, item.analyzedItem().aiDescription(), conn);
        });
    }

    private static Long insertWardrobeItem(WardrobeItem item, Connection conn) throws SQLException {
        String sql = "INSERT INTO apparel (user_name, image_url, category, color, brand) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, item.userName());
            stmt.setString(2, item.fileName());
            stmt.setString(3, item.category());
            stmt.setString(4, item.color());
            stmt.setString(5, item.brand());

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Wardrobe item inserted. Rows affected: " + rowsAffected);

            // Retrieve the auto-generated ID
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if(generatedKeys.next()) {
                    long itemId = generatedKeys.getLong(1);
                    System.out.println("Generated wardrobe item ID: " + itemId);
                    return itemId;
                }
            }
        }

        return null;
    }

    private void insertWardrobeAnalyzedItem(Long itemId, String aiDescription, Connection conn) throws SQLException {
        String sql = "INSERT INTO apparel_analysis (apparel_id, ai_description) VALUES (?, to_jsonb(?::text))";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, itemId);
            stmt.setString(2, aiDescription);

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Wardrobe analyzed item inserted. Rows affected: " + rowsAffected);
        }
    }


    /**
     * Get all wardrobe items for a user with most recent items first
     */
    public List<WardrobeItem> getWardrobeItemsByUserId(String userName) throws Exception {
        return executeQuery(conn -> {
            String sql = "SELECT apparel_id, user_name, image_url, category, color, brand FROM apparel WHERE user_name = ? ORDER BY apparel_id DESC";
            List<WardrobeItem> items = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while(rs.next()) {
                        items.add(new WardrobeItem(
                                rs.getString("apparel_id"),
                                rs.getObject("user_name", UUID.class),
                                rs.getString("image_url"),
                                rs.getString("category"),
                                rs.getString("color"),
                                rs.getString("brand"),
                                null
                        ));
                    }
                }
            }
            return items;
        });
    }

    /**
     * Get all wardrobe items for a user by category with most recent items first
     */
    public List<WardrobeItem> getWardrobeItemsByUserNameAndCategory(UUID userName, String category) throws Exception {
        return executeQuery(conn -> {
            String sql = "SELECT apparel_id, user_name, image_url, category, color, brand FROM apparel WHERE user_name = ? AND category = ? ORDER BY apparel_id DESC";
            List<WardrobeItem> items = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, userName);
                stmt.setString(2, category);

                try (ResultSet rs = stmt.executeQuery()) {
                    while(rs.next()) {
                        items.add(new WardrobeItem(
                                rs.getString("apparel_id"),
                                rs.getObject("user_name", UUID.class),
                                rs.getString("image_url"),
                                rs.getString("category"),
                                rs.getString("color"),
                                rs.getString("brand"),
                                null
                        ));
                    }
                }
            }
            return items;
        });
    }

    /**
     * Update a wardrobe item by user name
     */
    public void updateWardrobeItem(String id, String category, String color, String brand, String userName) throws Exception {
        executeTransaction(conn -> {
            String sql = "UPDATE apparel SET category = ?, color = ?, brand = ?, modified_at = CURRENT_TIMESTAMP WHERE apparel_id = ? AND user_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, category);
                stmt.setString(2, color);
                stmt.setString(3, brand);
                stmt.setString(4, id);

                int rowsAffected = stmt.executeUpdate();
                System.out.println("Wardrobe item updated. Rows affected: " + rowsAffected);
            }
        });
    }

    /**
     * Delete a wardrobe item by id and userName (ownership validation)
     *
     * @param id       wardrobe item id
     * @param userName user name (for security validation)
     * @throws Exception if deletion fails
     */
    public void deleteWardrobeItem(String id, UUID userName) throws Exception {
        executeTransaction(conn -> {
            String sql = "DELETE FROM apparel WHERE apparel_id = ? AND user_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.setObject(2, userName);

                int rowsAffected = stmt.executeUpdate();
                if(rowsAffected == 0) {
                    throw new Exception("Wardrobe item not found or unauthorized access");
                }
                System.out.println("Wardrobe item deleted. Rows affected: " + rowsAffected);
            }
        });
    }
}
