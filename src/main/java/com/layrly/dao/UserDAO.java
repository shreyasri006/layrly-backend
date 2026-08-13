package com.layrly.dao;

import com.layrly.domain.User;

import java.util.UUID;

/**
 * Data Access Object contract for users table.
 */
public interface UserDAO {
    void insertUser(UUID userName, String name, String email, String gender, String zip) throws Exception;

    boolean userExists(UUID userName) throws Exception;

    User getUserByUsername(UUID userName) throws Exception;
}
