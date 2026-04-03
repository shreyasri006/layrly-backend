package com.layrly.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class AbstractLambdaHandlerTest {
    protected ObjectMapper mapper = new ObjectMapper();

    protected APIGatewayProxyRequestEvent getApiGatewayProxyRequestEvent() throws IOException {
        APIGatewayProxyRequestEvent event = mapper.readValue(WardrobeLambdaHandlerTest.class.getResourceAsStream(
                "/create-wardrobe-item.json"), APIGatewayProxyRequestEvent.class);
        return event;
    }
}
