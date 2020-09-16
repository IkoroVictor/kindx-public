package io.kindx.tests;

import com.amazonaws.*;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.http.*;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.GetRestApisRequest;
import com.amazonaws.services.apigateway.model.RestApi;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClientBuilder;
import com.amazonaws.services.cognitoidentity.model.*;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.kindx.tests.util.TestUtil;
import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.kindx.tests.util.EnvUtil.getEnv;
import static io.kindx.tests.util.EnvUtil.getEnvOrDefault;
import static io.kindx.tests.util.TestUtil.logAWSResponse;
import static io.kindx.tests.util.TestUtil.waitForProcessing;
import static io.restassured.RestAssured.given;

public abstract class BaseClusterTests {

    @ClassRule
    public static final EnvironmentVariables envVariables = TestUtil.loadTestEnvVariables(
            String.format("env/%s.env", getEnvOrDefault("ENVIRONMENT", "stage")));

    private static final String ADMIN_API_NAME = "AdminApi";
    private static final String WEBHOOKS_API_NAME = "WebhooksApi";
    private static final String FRONT_API_NAME = "FrontApi";
    private static final Map<String, String> URLS_MAP = new HashMap<>();

    private static AuthenticationResultType ADMIN_CREDENTIALS;
    private static GetCredentialsForIdentityResult FRONT_CREDENTIALS;

    protected final long SLEEP_PERIOD_MILLISECONDS = 25000;

    static  {
        RestAssured.filters(
                new RequestLoggingFilter(),
                new ResponseLoggingFilter(),
                new ErrorLoggingFilter()
        );
        RestAssured.config = new RestAssuredConfig()
                .encoderConfig(new EncoderConfig("UTF-8","UTF-8"))
                .decoderConfig(new DecoderConfig("UTF-8"));
    }


    protected ObjectMapper objectMapper;

    private AWSCognitoIdentityProvider awsCognitoIdentityProvider;
    private AmazonApiGateway amazonApiGateway;
    private AmazonCognitoIdentity amazonCognitoIdentity;
    private AmazonHttpClient amazonHttpClient;
    private String region;
    private String stage;

    private HttpResponseHandler<String> responseHandler;
    private HttpResponseHandler<ResponseException> exceptionResponseHandler;


    @Before
    public void init() {
        region = getEnvOrDefault("REGION", "us-east-1");
        stage = getEnvOrDefault("API_STAGE", "STAGE");

        amazonApiGateway = getGatewayClient();
        amazonCognitoIdentity = getAmazonCognitoIdentity();
        awsCognitoIdentityProvider = getAWSCognitoIdentityProvider();
        amazonHttpClient = getAmazonHttpClient();
        objectMapper = new ObjectMapper();
        responseHandler = new HttpResponseHandler<String>() {
            @Override
            public String handle(HttpResponse response) throws Exception {
                return response.getContent() != null
                        ? IOUtils.toString(response.getContent(), StandardCharsets.UTF_8)
                        : null;
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return false;
            }
        };

        exceptionResponseHandler = new HttpResponseHandler<ResponseException>() {
            @Override
            public ResponseException handle(HttpResponse response) throws Exception {
                return new ResponseException(response);
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return false;
            }
        };
        setup();
    }

    @After
    public void cleanup() throws Exception {
        if (FRONT_CREDENTIALS != null) {
            amazonCognitoIdentity.deleteIdentities(new DeleteIdentitiesRequest()
                    .withIdentityIdsToDelete(FRONT_CREDENTIALS.getIdentityId()));
        }
        tearDown();
        waitForProcessing(5000); //Wait for GSIs to be updated
    }

    protected abstract void setup();
    protected abstract void tearDown();



    protected String getAdminUrl() {
       return getDeploymentUrl(ADMIN_API_NAME);
    }

    protected String getWebhooksUrl() {
        return getDeploymentUrl(WEBHOOKS_API_NAME);
    }

    protected String getFrontUrl() {
        return getDeploymentUrl(FRONT_API_NAME);
    }


    private String getDeploymentUrl(String restApiName) {
        if(!URLS_MAP.containsKey(restApiName)) {
            RestApi api = amazonApiGateway
                    .getRestApis(new GetRestApisRequest())
                    .getItems()
                    .stream()
                    .filter(f -> f.getName().equalsIgnoreCase(restApiName))
                    .findAny().orElseThrow(() -> new IllegalArgumentException("API not found"));

            URLS_MAP.put(restApiName, "https://" + api.getId() + ".execute-api.us-east-1.amazonaws.com/" + stage);
        }
        return URLS_MAP.get(restApiName);

    }

    protected String createUnauthenticatedUserId() {
        return amazonCognitoIdentity.getId(new GetIdRequest()
                .withIdentityPoolId(getEnv("FRONT_ID_POOL")))
                .getIdentityId();
    }

    protected AuthenticationResultType getAdminUserCredentials() {
        if (ADMIN_CREDENTIALS == null) {
            ADMIN_CREDENTIALS = awsCognitoIdentityProvider.initiateAuth(new InitiateAuthRequest()
                    .withAuthFlow(AuthFlowType.USER_PASSWORD_AUTH).withAuthParameters(
                            ImmutableMap.of(
                                    "USERNAME", getEnv("ADMIN_USERNAME"),
                                    "PASSWORD", getEnv("ADMIN_PASSWORD"))
                    )
                    .withClientId(getEnv("ADMIN_POOL_CLIENT_ID")))
                    .getAuthenticationResult();
        }
        return ADMIN_CREDENTIALS;
    }


    protected GetCredentialsForIdentityResult getFrontUserCredentials() {
        if (FRONT_CREDENTIALS == null) {
            FRONT_CREDENTIALS = amazonCognitoIdentity.getCredentialsForIdentity(
                    new GetCredentialsForIdentityRequest()
                            .withIdentityId(createUnauthenticatedUserId()));
        }
        return FRONT_CREDENTIALS;
    }


    protected Response<String> executeSignedRequest(Request request) {
        Credentials credentials = getFrontUserCredentials().getCredentials();
        AWS4Signer signer = new AWS4Signer();
        signer.setRegionName(region);
        signer.setServiceName(request.getServiceName());
        signer.sign(request,
                new BasicSessionCredentials(credentials.getAccessKeyId(),
                        credentials.getSecretKey(),
                        credentials.getSessionToken()));
        try {
            return amazonHttpClient
                    .requestExecutionBuilder()
                    .executionContext(new ExecutionContext(true))
                    .request(request)
                    .errorResponseHandler(exceptionResponseHandler)
                    .execute(responseHandler);
        } catch (ResponseException ex) {
            return new Response<>(null, ex.getResponse());
        }
    }


    protected DefaultRequest createFrontApiSignableRequest(HttpMethodName methodName,
                                                           String resourcePath,
                                                           String body,
                                                           Map<String, String> queryParams,
                                                           Map<String, String> headers) {
        DefaultRequest<?> signableRequest = new DefaultRequest<>("execute-api");
        signableRequest.setHttpMethod(methodName);
        signableRequest.setResourcePath(resourcePath);
        signableRequest.setEndpoint(URI.create(getFrontUrl()));
        signableRequest.addHeader("Content-Type", "application/json");
        if (body != null) {
            signableRequest.setContent(new ByteArrayInputStream(body.getBytes()));
        }

        if(queryParams != null) {
            queryParams.forEach(signableRequest::addParameter);
        }

        if(headers != null) {
            headers.forEach(signableRequest::addHeader);
        }
        return signableRequest;
    }

    private AmazonApiGateway getGatewayClient() {
        return AmazonApiGatewayClientBuilder
                .standard()
                .withRegion(region)
                .build();
    }


    private AmazonHttpClient getAmazonHttpClient() {
        return AmazonHttpClient.builder()
                .clientConfiguration(new ClientConfiguration()
                        .withRetryPolicy(ClientConfiguration.DEFAULT_RETRY_POLICY))
                .build();
    }

    private AmazonCognitoIdentity getAmazonCognitoIdentity() {
        return AmazonCognitoIdentityClientBuilder
                .standard()
                .withRegion(region)
                .build();
    }

    private AWSCognitoIdentityProvider getAWSCognitoIdentityProvider() {
        return AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withRegion(region)
                .build();
    }

    protected Response<String> getKitchenMenus(String kitchenId) {
        //Fetch menu
        DefaultRequest request = createFrontApiSignableRequest(HttpMethodName.GET,
                "/menus",
                null,
                Collections.singletonMap("kitchen", kitchenId),
                null);

        Response<String> response = executeSignedRequest(request);
        logAWSResponse(response);
        return response;
    }


    protected String getFixtureMenuText() {
        return TestUtil.loadTextFileResource("usermapping_menu_plaintext.txt");
    }

    protected String createKitchen(Map body) {
        return given()
                .body(body)
                .header("Authorization", getAdminUserCredentials().getIdToken())
                .when()
                .post(getAdminUrl()+ "/kitchens")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    protected Response<String> putUserKitchenMapping(String kitchenId, Map mappingBody) throws Exception {
        return executeUserKitchenMapping(HttpMethodName.PUT,
                kitchenId,
                objectMapper.writeValueAsString(mappingBody));
    }

    protected Response<String> deleteUserKitchenMapping(String kitchenId) throws Exception {
       return executeUserKitchenMapping(HttpMethodName.DELETE, kitchenId, null);
    }

    protected Response<String> getUserKitchenMapping(String kitchenId) throws Exception {
        return executeUserKitchenMapping(HttpMethodName.GET, kitchenId, null);
    }

    private Response<String> executeUserKitchenMapping(HttpMethodName methodName,
                                                        String kitchenId, String body)  {
        DefaultRequest request = createFrontApiSignableRequest(methodName,
                "/users/kitchens/" + kitchenId,
                body,
                null,
                null);

        Response<String> response = executeSignedRequest(request);
        logAWSResponse(response);
        return response;
    }


    public static final class ResponseException extends AmazonClientException {
        private HttpResponse response;

        public ResponseException(HttpResponse response) {
            super(response.getStatusCode() + " - " + response.getStatusText());
            this.response = response;
        }

        public HttpResponse getResponse() {
            return response;
        }
    }

}
