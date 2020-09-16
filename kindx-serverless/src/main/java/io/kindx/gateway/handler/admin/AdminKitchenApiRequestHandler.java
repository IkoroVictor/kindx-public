package io.kindx.gateway.handler.admin;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.service.EventService;
import io.kindx.constants.Paths;
import io.kindx.factory.InjectorFactory;
import io.kindx.gateway.dto.KitchenCreateDto;
import io.kindx.gateway.dto.KitchenDto;
import io.kindx.gateway.dto.KitchenUpdateDto;
import io.kindx.gateway.facade.admin.AdminKitchenFacade;
import io.kindx.gateway.handler.BaseRequestHandler;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.bigtesting.routd.Route;

import java.util.ArrayList;
import java.util.List;

public class AdminKitchenApiRequestHandler extends BaseRequestHandler {

    private final AdminKitchenFacade facade;

    private static final String KITCHEN_PATH_PARAM = "kitchenId";
    private static final String FACEBOOK_ID_PATH_PARAM = "pageId";
    private static final String PLACES_ID_PATH_PARAM = "placesId";

    public AdminKitchenApiRequestHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class));
        this.facade = InjectorFactory.getInjector().getInstance(AdminKitchenFacade.class);
    }


    @SneakyThrows
    private APIGatewayProxyResponseEvent handleCreate(Route route, APIGatewayProxyRequestEvent input, Context context) {
        KitchenCreateDto kitchenCreateDto = getRequestBody(input, KitchenCreateDto.class);
        String creator = getIdentity(input);
        KitchenDto contentDto = facade.createKitchen(kitchenCreateDto, creator);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_CREATED)
                .withBody(objectMapper.writeValueAsString(contentDto));
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleUpdate(Route route, APIGatewayProxyRequestEvent input, Context context) {
        String kitchenId = getRoutePathParams(route, input).get(KITCHEN_PATH_PARAM);
        KitchenUpdateDto dto = getRequestBody(input, KitchenUpdateDto.class);
        validateDto(dto);
        String updater = getIdentity(input);
        KitchenDto contentDto = facade.updateKitchen(dto, kitchenId, updater);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(objectMapper.writeValueAsString(contentDto));
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleDelete(Route route, APIGatewayProxyRequestEvent input, Context context) {
        String kitchenId = getRoutePathParams(route, input).get(KITCHEN_PATH_PARAM);
        facade.deleteKitchen(kitchenId);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_NO_CONTENT);
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleProcess(Route route,
                                                       APIGatewayProxyRequestEvent input,
                                                       Context context) {
        String kitchenId = getRoutePathParams(route, input).get(KITCHEN_PATH_PARAM);
        facade.reprocessMenusForKitchen(kitchenId);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_NO_CONTENT);
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGet(Route route, APIGatewayProxyRequestEvent input, Context context) {
        String kitchenId = getRoutePathParams(route, input).get(KITCHEN_PATH_PARAM);
        KitchenDto contentDto = facade.fetchKitchen(kitchenId);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(objectMapper.writeValueAsString(contentDto));
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGetByPlaceId(Route route, APIGatewayProxyRequestEvent input, Context context) {
        String id = getRoutePathParams(route, input).get(PLACES_ID_PATH_PARAM);
        KitchenDto contentDto = facade.fetchKitchenByPlacesId(id);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(objectMapper.writeValueAsString(contentDto));
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGetByFacebookId(Route route, APIGatewayProxyRequestEvent input, Context context) {
        String id = getRoutePathParams(route, input).get(FACEBOOK_ID_PATH_PARAM);
        KitchenDto contentDto = facade.fetchKitchenByFacebookPageId(id);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(objectMapper.writeValueAsString(contentDto));
    }

    @Override
    protected @NonNull List<RouteMapping> allRoutes() {
        List<RouteMapping> routeMappings = new ArrayList<>();
        routeMappings.add(RouteMapping
                .builder()
                .path(Paths.ADMIN_KITCHENS_PATH)
                .method("POST")
                .handler(this::handleCreate)
                .build());

        routeMappings.add(RouteMapping
                .builder()
                .path(Paths.ADMIN_KITCHENS_PATH + "/:kitchenId")
                .method("PATCH")
                .handler(this::handleUpdate)
                .build());

        routeMappings.add(RouteMapping
                .builder()
                .path(Paths.ADMIN_KITCHENS_PATH + "/:kitchenId")
                .method("GET")
                .handler(this::handleGet)
                .build());
        routeMappings.add(RouteMapping
                .builder()
                .path(Paths.ADMIN_KITCHENS_PATH + "/:kitchenId")
                .method("DELETE")
                .handler(this::handleDelete)
                .build());
        routeMappings.add(RouteMapping
                .builder()
                .path(Paths.ADMIN_KITCHENS_PATH + "/:kitchenId/process")
                .method("POST")
                .handler(this::handleProcess)
                .build());
        routeMappings.add(RouteMapping
                .builder()
                .path(Paths.ADMIN_KITCHENS_PATH + "/facebook/:pageId")
                .method("GET")
                .handler(this::handleGetByFacebookId)
                .build());

        routeMappings.add(RouteMapping
                .builder()
                .path(Paths.ADMIN_KITCHENS_PATH + "/google/:placesId")
                .method("GET")
                .handler(this::handleGetByPlaceId)
                .build());
        return routeMappings;
    }

    @Override
    protected @NonNull String basePath() {
        return Paths.ADMIN_BASE_PATH;
    }
}
