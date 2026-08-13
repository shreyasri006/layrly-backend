package com.layrly.dao;

import com.layrly.domain.WardrobeItem;

import java.util.List;
import java.util.UUID;

/**
 * Data Access Object contract for Wardrobe Items table.
 */
public interface WardrobeItemDAO {
    void insertWardrobeItem(WardrobeItem item) throws Exception;

    List<WardrobeItem> getWardrobeItemsByUserId(String userName) throws Exception;

    List<WardrobeItem> getWardrobeItemsByUserNameAndCategory(String userName, String category) throws Exception;

    void updateWardrobeItem(String apparelId, String category, String color, String brand, UUID userName) throws Exception;

    void deleteWardrobeItem(String id, UUID userName) throws Exception;

    int getApparelCountByUserName(String userName) throws Exception;
}
