package io.kindx.gateway.handler.webhook;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.service.EventService;
import io.kindx.constants.Paths;
import io.kindx.dto.facebook.webhook.FacebookWebhookEventDto;
import io.kindx.factory.InjectorFactory;
import io.kindx.gateway.facade.webhook.FacebookWebhookFacade;
import io.kindx.gateway.handler.BaseRequestHandler;
import lombok.NonNull;
import org.apache.http.HttpStatus;
import org.bigtesting.routd.Route;

import java.util.ArrayList;
import java.util.List;

public class FacebookWebhookRequestHandler extends BaseRequestHandler {

    private FacebookWebhookFacade facade;

    public FacebookWebhookRequestHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class));
        facade = InjectorFactory.getInjector().getInstance(FacebookWebhookFacade.class);
    }


    private APIGatewayProxyResponseEvent handleVerify(Route route,
                                                     APIGatewayProxyRequestEvent requestEvent,
                                                     Context context) {
       String challenge = facade.verifyWebhook(requestEvent.getQueryStringParameters());
       return new APIGatewayProxyResponseEvent().withBody(challenge)
               .withStatusCode(HttpStatus.SC_OK)
               .withBody(challenge);
    }

    private APIGatewayProxyResponseEvent handleEvent(Route route,
                                                      APIGatewayProxyRequestEvent requestEvent,
                                                      Context context) {
        FacebookWebhookEventDto event = getRequestBody(requestEvent, FacebookWebhookEventDto.class);
        facade.processWebhookEvent(event);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_OK);
    }


    @Override
    protected @NonNull List<RouteMapping> allRoutes() {
        List<RouteMapping> routeMappings = new ArrayList<>();
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FACEBOOK_WEBHOOK_PATH)
                .method("GET")
                .handler(this::handleVerify)
                .build());
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FACEBOOK_WEBHOOK_PATH)
                .method("POST")
                .handler(this::handleEvent)
                .build());
        return routeMappings;
    }

    @Override
    protected @NonNull String basePath() {
        return "";
    }
}
