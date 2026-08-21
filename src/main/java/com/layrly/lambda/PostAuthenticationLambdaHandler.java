package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.layrly.dao.DAOFactory;
import com.layrly.dao.LoginHistoryDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PostAuthenticationLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(PostAuthenticationLambdaHandler.class);

    private static LoginHistoryDAO historyDAO = DAOFactory.getDao(LoginHistoryDAO.class);

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            // log.info("Received event: {}", mapper.writeValueAsString(event));

            var userNameObj = event.get("userName");

            if(userNameObj != null) {
                historyDAO.insert(userNameObj.toString());
            }
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        return event;
    }
}
