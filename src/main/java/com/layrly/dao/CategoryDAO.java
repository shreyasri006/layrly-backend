package com.layrly.dao;

import com.layrly.domain.Category;

import java.util.List;

/**
 * Data Access Object contract for Category table.
 */
public interface CategoryDAO {
    /**
     * Get all categories.
     */
    List<Category> getAllCategories() throws Exception;

    /**
     * Get total count of categories.
     */
    int getTotalCategories() throws Exception;
}
