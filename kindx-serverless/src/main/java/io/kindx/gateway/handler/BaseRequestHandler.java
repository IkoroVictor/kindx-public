package io.kindx.gateway.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.UsageEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.constants.UsageEventSource;
import io.kindx.dto.BaseDto;
import io.kindx.exception.NotFoundException;
import io.kindx.gateway.dto.ErrorDto;
import io.kindx.gateway.exception.InvalidRequestException;
import io.kindx.gateway.exception.MethodNotAllowedException;
import io.kindx.gateway.handler.error.ResponseExceptionHandler;
import io.kindx.metrics.MetricsLogger;
import io.kindx.util.IDUtil;
import io.kindx.util.LogUtils;
import io.kindx.util.TriFunction;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bigtesting.routd.NamedParameterElement;
import org.bigtesting.routd.Route;
import org.bigtesting.routd.Router;
import org.bigtesting.routd.TreeRouter;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.*;

public abstract class BaseRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LogManager.getLogger(BaseRequestHandler.class);

    private Router router;
    private Map<Route, Set<String>> routesMethodsMap;
    private Map<String, TriFunction<Route, APIGatewayProxyRequestEvent, Context, APIGatewayProxyResponseEvent>> routesHandlerMap;
    private ResponseExceptionHandler exceptionHandler;
    private final Validator validator;

    protected final ObjectMapper objectMapper;
    private EventService eventService;


    public BaseRequestHandler(ObjectMapper objectMapper,
                              EventService eventService) {
        this.objectMapper = objectMapper;
        this.eventService = eventService;
        this.router = new TreeRouter();
        this.routesMethodsMap = new HashMap<>();
        this.routesHandlerMap = new HashMap<>();
        this.exceptionHandler = new ResponseExceptionHandler();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        addPingRoute();
        allRoutes().forEach(r -> addRoute(r.getMethod(), r.getPath(), r.getHandler()));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        LogUtils.setCorrelationId(IDUtil.generateCorrelationId());
        long start = System.currentTimeMillis();
        APIGatewayProxyResponseEvent responseEvent = processProxyRequest(input, context);
        enableCors(responseEvent);
        sendUsageEventAndMetrics(input, responseEvent, System.currentTimeMillis() -  start);
        return responseEvent;
    }

    private void enableCors(APIGatewayProxyResponseEvent responseEvent) {
        Map<String, String> headers = new HashMap<>();
        if (responseEvent.getHeaders() != null) {
            headers.putAll(responseEvent.getHeaders());
        }
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "*");
        headers.put("Access-Control-Allow-Methods", "*");
        responseEvent.setHeaders(headers);
    }

    private APIGatewayProxyResponseEvent processProxyRequest(APIGatewayProxyRequestEvent input, Context context) {
        logRequest(input);
        Route route = router.route(getPath(input));
        if(route == null){
            return buildErrorResponse(new NotFoundException("Path not found"));
        }
        if (!routesMethodsMap.get(route).contains(input.getHttpMethod())) {
            return buildErrorResponse(new MethodNotAllowedException("Method not allowed"));
        }
        TriFunction function = routesHandlerMap.get(buildHandlerKey(route, input.getHttpMethod()));
        try {
            return (APIGatewayProxyResponseEvent) function.apply(route, input, context);
        } catch (Exception ex) {
            return buildErrorResponse(ex);
        }
    }

    protected Map<String, String> getRoutePathParams(Route route, APIGatewayProxyRequestEvent request) {
        Map<String, String> params = new HashMap<>();
        String path = getPath(request);
        for (NamedParameterElement p : route.getNamedParameterElements()) {
            if (p != null) {
                params.put(p.name(), route.getNamedParameter(p.name(), path));
            }
        }
        return params;
    }


    private String getPath(APIGatewayProxyRequestEvent request) {
        return stripBasePath(request.getPath());
    }

    protected void addRoute(String httpMethod,
                            String path,
                            TriFunction<Route, APIGatewayProxyRequestEvent, Context, APIGatewayProxyResponseEvent> handler) {
        validateMethodAndPath(httpMethod, path);
        Route route = new Route(path);
        routesMethodsMap.putIfAbsent(route, new HashSet<>());
        routesMethodsMap.get(route).add(httpMethod);
        routesHandlerMap.put(buildHandlerKey(route, httpMethod), handler);
        router.add(route);
    }

    private void sendUsageEventAndMetrics(APIGatewayProxyRequestEvent requestEvent,
                                          APIGatewayProxyResponseEvent responseEvent,
                                          long latency) {
        UsageEvent.Status status =  (responseEvent.getStatusCode() / 100 != 2)
                ? UsageEvent.Status.FAILED : UsageEvent.Status.SUCCESS;

        Map<String, Object> meta = new HashMap<>();
        meta.put("latencyInMs", latency);
        meta.put("method", requestEvent.getHttpMethod().toUpperCase());
        meta.put("path", requestEvent.getPath());
        meta.put("responseCode", responseEvent.getStatusCode());
        meta.put("queryParams", requestEvent.getQueryStringParameters());
        meta.put("pathParams", requestEvent.getPathParameters());

        String responseStatus = String.format("%dxx", responseEvent.getStatusCode() / 100);
        MetricsLogger.logMetrics(this.getClass(), latency,"request", Collections.singletonMap("status", responseStatus));

        eventService.publishUsageEvent(UsageEvent.builder()
                .eventId(IDUtil.generateUsageId())
                .correlationId(LogUtils.getCorrelationId())
                .createdTimestamp(new Date().getTime())
                .status(status)
                .source(UsageEventSource.API_REQUEST)
                .actor(getIdentity(requestEvent))
                .meta(meta)
                .build());
    }


    @NonNull
    protected abstract List<RouteMapping> allRoutes();

    @NonNull
    protected abstract String basePath();


    protected String getIdentity(APIGatewayProxyRequestEvent input) {
        //TODO: Should fail if request doesn't have identity. Till then return 'anonymous'
        String id = null;
        APIGatewayProxyRequestEvent.RequestIdentity identity = input.getRequestContext().getIdentity();
        if (identity != null) {
            id = identity.getCognitoIdentityId();
        }
        return id;
    }


    private void validateMethodAndPath(String httpMethod, String path) {
        Route r = router.route(path);
        if (r != null && routesMethodsMap.get(r).contains(httpMethod)){
            throw new RuntimeException(String.format("path '%s' and method '%s' already exists", path, httpMethod));
        }
    }
    private String buildHandlerKey(Route route, String method) {
        return String.format("%d-%s", route.hashCode(), method);
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent buildErrorResponse(Exception ex) {
        ErrorDto dto = exceptionHandler.handleException(ex);
        return new  APIGatewayProxyResponseEvent()
                .withStatusCode(dto.getCode())
                .withIsBase64Encoded(false)
                .withBody(objectMapper.writeValueAsString(dto));
    }

    private void addPingRoute() {
        Route route = new Route("/ping");
        routesMethodsMap.putIfAbsent(route, new HashSet<>());
        routesMethodsMap.get(route).add("GET");
        routesMethodsMap.get(route).add("POST");
        routesHandlerMap.put(buildHandlerKey(route, "GET"), (x, y, z) -> new APIGatewayProxyResponseEvent()
                .withStatusCode(200));
        routesHandlerMap.put(buildHandlerKey(route, "POST"), (x, y, z) -> new APIGatewayProxyResponseEvent()
                .withStatusCode(200));
        router.add(route);
    }

    protected  <T extends BaseDto> void validateDto(T dto)  {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        if (violations.size() > 0) {
            ConstraintViolation violation = violations.iterator().next();
            throw new InvalidRequestException(String.format("[%s]:  %s",
                    violation.getPropertyPath(),
                    violation.getMessage()));
        }
    }

    private void logRequest(APIGatewayProxyRequestEvent requestEvent) {
        logger.info("REQ: [{}] - [{}] - [{}] - [{}]" ,
                requestEvent.getHttpMethod(),
                requestEvent.getPath(),
                requestEvent.getResource(),
                LogUtils.getCorrelationId());
    }

    private String stripBasePath(String path) {
        if (path.startsWith(basePath())) {
            return path.replaceFirst(basePath(), "");
        }
        return path;
    }


    protected <T extends BaseDto> T getRequestBody(APIGatewayProxyRequestEvent event, Class<T> tClass) {
        T body;
        try {
            logger.info(event.getBody());
            body = objectMapper.readValue(event.getBody(), tClass);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException("Invalid request body");
        }
        validateDto(body);
        return body;
    }

    @Getter
    @Builder
    protected static final class RouteMapping {
        @NonNull
        private String method;
        @NonNull
        private String path;
        @NonNull
        private TriFunction<Route, APIGatewayProxyRequestEvent, Context, APIGatewayProxyResponseEvent> handler;
    }




}
