package com.layrly.dao;

import com.layrly.domain.Category;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Category table
 */
public class CategoryDAO extends BaseDAO {
    /**
     * Get all categories
     */
    public List<Category> getAllCategories() throws Exception {
        return executeQuery(conn -> {
            String sql = "SELECT id, name, display_order FROM category ORDER BY display_order";
            List<Category> categories = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while(rs.next()) {
                        categories.add(new Category(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getInt("display_order")
                        ));
                    }
                }
            }
            return categories;
        });
    }

    /**
     * Get total count of category
     *
     * @return count of Category
     * @throws Exception if query fails
     */
    public long getTotalCategories() throws Exception {
        return executeQuery(conn -> {
            String sql = "SELECT COUNT(*) as count FROM category";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                try (ResultSet rs = stmt.executeQuery()) {
                    if(rs.next()) {
                        return rs.getLong("count");
                    }
                }
            }
            return 0L;
        });
    }
}
