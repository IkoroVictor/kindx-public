package io.kindx.tests.front;

import com.amazonaws.DefaultRequest;
import com.amazonaws.Response;
import com.amazonaws.http.HttpMethodName;
import com.google.common.collect.ImmutableMap;
import io.kindx.tests.BaseClusterTests;
import io.restassured.path.json.JsonPath;
import org.junit.Test;

import java.util.Arrays;

import static io.kindx.tests.util.TestUtil.logAWSResponse;
import static org.junit.Assert.*;

public class UserTests extends BaseClusterTests {

    @Override
    protected void setup() {
    }

    @Override
    protected void tearDown() { }

    @Test
    public void testUserGet() {
        DefaultRequest request = createFrontApiSignableRequest(HttpMethodName.GET,
                "/users/me" ,
                null,
                null,
                null);

        Response<String> response = executeSignedRequest(request);
        logAWSResponse(response);

        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertNotNull(path.getString("userId"));
        assertEquals("NONE", path.getString("notificationChannel"));
        assertEquals("en-gb", path.getString("locale"));
        assertNotNull(path.getList("locations"));
        assertTrue(path.getList("locations").size() > 0);
        assertNull(path.getList("generalFoodPreferences"));
        assertNull(path.getList("userLastLocation"));
    }


    @Test
    public void testUserUpdate() throws Exception {
        DefaultRequest request = createFrontApiSignableRequest(HttpMethodName.GET,
                "/users/me" ,
                null,
                null,
                null);

        Response<String> response = executeSignedRequest(request);
        logAWSResponse(response);

        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertNotNull(path.getString("userId"));
        assertEquals("en-gb", path.getString("locale"));
        assertNull(path.getList("generalFoodPreferences"));


        request = createFrontApiSignableRequest(HttpMethodName.PUT,
                "/users/me",
                objectMapper.writeValueAsString(ImmutableMap.of(
                        "generalFoodPreferences", Arrays.asList("milk", "bread", "piim"),
                        "locale", "sv-se"
                )),
                null,
                null);

        response = executeSignedRequest(request);
        logAWSResponse(response);

        assertEquals(200, response.getHttpResponse().getStatusCode());

        path = JsonPath.from(response.getAwsResponse());
        assertNotNull(path.getString("userId"));
        assertEquals("sv-se", path.getString("locale"));
        assertNotNull(path.getList("generalFoodPreferences"));
        assertEquals(3, path.getList("generalFoodPreferences").size());

        assertTrue(path.getList("generalFoodPreferences").contains("milk"));
        assertTrue(path.getList("generalFoodPreferences").contains("piim"));
        assertTrue(path.getList("generalFoodPreferences").contains("bread"));
    }
}
