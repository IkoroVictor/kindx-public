package io.kindx.gateway.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.kindx.backoffice.service.EventService;
import io.kindx.constants.Paths;
import io.kindx.factory.InjectorFactory;
import io.kindx.gateway.dto.*;
import io.kindx.gateway.facade.MenuFacade;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.bigtesting.routd.Route;

import java.util.ArrayList;
import java.util.List;

public class MenuApiRequestHandler extends BaseRequestHandler {

    private static final String MENU_PATH_PARAM = "menuId";
    private final MenuFacade facade;

    @Inject
    public MenuApiRequestHandler () {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class));
        this.facade = InjectorFactory.getInjector().getInstance(MenuFacade.class);
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleSearch(Route route, APIGatewayProxyRequestEvent input, Context context) {
        MenuSearchDto searchDto = getRequestBody(input, MenuSearchDto.class);
        PaginatedContentDto<MenuDto> contentDto = facade.search(searchDto);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(objectMapper.writeValueAsString(contentDto));
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGet(Route route, APIGatewayProxyRequestEvent input, Context context) {
        MenuQueryDto queryDto = MenuQueryDto.builder()
                .kitchenId(input.getQueryStringParameters().get("kitchen"))
                .pageToken(input.getQueryStringParameters().get("pageToken"))
                .build();
        PaginatedContentDto<MenuDto> contentDto = facade.getMenus(queryDto, getIdentity(input));
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(objectMapper.writeValueAsString(contentDto));
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGetSingle(Route route, APIGatewayProxyRequestEvent input, Context context) {
        String menuId = getRoutePathParams(route, input).get(MENU_PATH_PARAM);
        MenuDto menu = facade.getMenu(menuId, getIdentity(input));
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(objectMapper.writeValueAsString(menu));
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleToday(Route route, APIGatewayProxyRequestEvent input, Context context) {
        String userId = getIdentity(input);
        MenuTodayQueryDto todayQueryDto = getRequestBody(input, MenuTodayQueryDto.class);
        PaginatedContentDto<MenuDto> contentDto = facade.getTodayMenus(todayQueryDto, userId);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(objectMapper.writeValueAsString(contentDto));
    }

    protected List<RouteMapping> allRoutes() {
        List<RouteMapping> routeMappings = new ArrayList<>();
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_MENU_SEARCH_PATH)
                .method("POST")
                .handler(this::handleSearch)
                .build());
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_MENU_TODAY_PATH)
                .method("POST")
                .handler(this::handleToday)
                .build());
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_MENU_PATH)
                .method("GET")
                .handler(this::handleGet)
                .build());
        routeMappings.add(RouteMapping.builder()
                .path(Paths.FRONT_MENU_PATH + "/:menuId" )
                .method("GET")
                .handler(this::handleGetSingle)
                .build());
        return routeMappings;
    }

    @Override
    protected @NonNull String basePath() {
        return Paths.FRONT_BASE_PATH;
    }
}
