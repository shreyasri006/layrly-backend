package com.layrly.dao;

/**
 * Data Access Object contract for login_history table.
 */
public interface LoginHistoryDAO {
    void insert(String userName) throws Exception;

    /**
     * Get continuous login streak count for a given user name.
     */
    int getRecentLoginsByUserName(String userName) throws Exception;
}
