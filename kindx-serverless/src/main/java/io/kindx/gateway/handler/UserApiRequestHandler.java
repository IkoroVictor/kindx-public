package io.kindx.gateway.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.kindx.backoffice.service.EventService;
import io.kindx.constants.Paths;
import io.kindx.dto.GeoPointDto;
import io.kindx.factory.InjectorFactory;
import io.kindx.gateway.dto.UserKitchenMappingCreateDto;
import io.kindx.gateway.dto.UserUpdateDto;
import io.kindx.gateway.facade.UserFacade;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bigtesting.routd.Route;

import java.util.ArrayList;
import java.util.List;

public class UserApiRequestHandler extends BaseRequestHandler {

    private static final String KITCHEN_PATH_PARAM = "kitchenId";
    private UserFacade userFacade;

    @Inject
    public UserApiRequestHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class));
        this.userFacade = InjectorFactory.getInjector().getInstance(UserFacade.class);
    }


    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGetUser(Route route,
                                                       APIGatewayProxyRequestEvent input,
                                                       Context context) {
        String userId = getIdentity(input);
        return new APIGatewayProxyResponseEvent()
                .withBody(objectMapper.writeValueAsString(userFacade.getUser(userId)))
                .withStatusCode(200);


    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleUpdateUser(Route route,
                                                          APIGatewayProxyRequestEvent input,
                                                          Context context) {
        String userId = getIdentity(input);
        UserUpdateDto dto = getRequestBody(input, UserUpdateDto.class);
        return new APIGatewayProxyResponseEvent()
                .withBody(objectMapper.writeValueAsString(userFacade.updateUser(userId, dto)))
                .withStatusCode(200);
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handlePutUserKitchenMapping(Route route,
                                                                 APIGatewayProxyRequestEvent input,
                                                                 Context context) {
        String kitchenId = getRoutePathParams(route, input).get(KITCHEN_PATH_PARAM);
        String userId = getIdentity(input);
        UserKitchenMappingCreateDto dto = getRequestBody(input, UserKitchenMappingCreateDto.class);
        userFacade.addUserKitchenMapping(userId, kitchenId, dto);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(202);
    }

    @SneakyThrows
    public APIGatewayProxyResponseEvent handleFetchUserKitchenMapping(Route route,
                                                                      APIGatewayProxyRequestEvent input,
                                                                      Context context) {
        String userId = getIdentity(input);
        String kitchenId = getRoutePathParams(route, input).get(KITCHEN_PATH_PARAM);
        return new APIGatewayProxyResponseEvent().withStatusCode(200)
                .withBody(objectMapper.writeValueAsString(userFacade.getUserKitchenMapping(userId, kitchenId)));
    }

    @SneakyThrows
    public APIGatewayProxyResponseEvent handleDeleteUserKitchenMapping(Route route,
                                                                      APIGatewayProxyRequestEvent input,
                                                                      Context context) {
        String userId = getIdentity(input);
        String kitchenId = getRoutePathParams(route, input).get(KITCHEN_PATH_PARAM);
        userFacade.deleteUserKitchenMapping(userId, kitchenId);
        return new APIGatewayProxyResponseEvent().withStatusCode(204);
    }

    @SneakyThrows
    public APIGatewayProxyResponseEvent handleLastLocation(Route route,
                                                           APIGatewayProxyRequestEvent input,
                                                           Context context) {
        String userId = getIdentity(input);
        GeoPointDto dto = getRequestBody(input, GeoPointDto.class);
        userFacade.addUserLastLocation(userId, dto);
        return new APIGatewayProxyResponseEvent().withStatusCode(202);
    }


    @Override
    protected @NonNull List<RouteMapping> allRoutes() {
        List<RouteMapping> routeMappings = new ArrayList<>();
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_USERS_KITCHENS_PATH + "/:kitchenId")
                .method("PUT")
                .handler(this::handlePutUserKitchenMapping)
                .build());

        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_USERS_KITCHENS_PATH + "/:kitchenId")
                .method("GET")
                .handler(this::handleFetchUserKitchenMapping)
                .build());

        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_USERS_KITCHENS_PATH + "/:kitchenId")
                .method("DELETE")
                .handler(this::handleDeleteUserKitchenMapping)
                .build());
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_USERS_LOCATION_PATH)
                .method("PUT")
                .handler(this::handleLastLocation)
                .build());
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_USERS_ME_PATH)
                .method("GET")
                .handler(this::handleGetUser)
                .build());
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_USERS_ME_PATH)
                .method("PUT")
                .handler(this::handleUpdateUser)
                .build());
        return routeMappings;
    }

    @Override
    protected @NonNull String basePath() {
        return Paths.FRONT_BASE_PATH;
    }
}
