package com.layrly.dao;

import com.layrly.dao.impl.CategoryDAOImpl;
import com.layrly.dao.impl.LoginHistoryDAOImpl;
import com.layrly.dao.impl.RecommendationDAOImpl;
import com.layrly.dao.impl.UserDAOImpl;
import com.layrly.dao.impl.WardrobeItemDAOImpl;

import java.util.HashMap;
import java.util.Map;

public final class DAOFactory {
    private static final Map<Class<?>, Object> DAO_IMPLEMENTATIONS = new HashMap<>();

    static {
        DAO_IMPLEMENTATIONS.put(CategoryDAO.class, new CategoryDAOImpl());
        DAO_IMPLEMENTATIONS.put(LoginHistoryDAO.class, new LoginHistoryDAOImpl());
        DAO_IMPLEMENTATIONS.put(RecommendationDAO.class, new RecommendationDAOImpl());
        DAO_IMPLEMENTATIONS.put(UserDAO.class, new UserDAOImpl());
        DAO_IMPLEMENTATIONS.put(WardrobeItemDAO.class, new WardrobeItemDAOImpl());
    }

    private DAOFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDao(Class<T> daoInterface) {
        return (T) DAO_IMPLEMENTATIONS.get(daoInterface);
    }
}
