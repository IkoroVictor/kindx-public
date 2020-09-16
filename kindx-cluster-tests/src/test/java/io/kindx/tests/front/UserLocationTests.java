package io.kindx.tests.front;

import com.amazonaws.DefaultRequest;
import com.amazonaws.Response;
import com.amazonaws.http.HttpMethodName;
import com.google.common.collect.ImmutableMap;
import io.kindx.tests.BaseClusterTests;
import io.restassured.path.json.JsonPath;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static io.kindx.tests.util.TestUtil.logAWSResponse;
import static io.kindx.tests.util.TestUtil.waitForProcessing;
import static org.junit.Assert.*;

public class UserLocationTests extends BaseClusterTests {

    private static final String FACEBOOK_PAGE_ID = "112134816798002";
    private Set<String> kitchensToCleanup = new HashSet<>();

    @Test
    @Ignore //TODO: Test with global preferences
    public void testUserLocationChangeReprocess() throws Exception {
        String randomPreference = RandomStringUtils.randomAlphabetic(9);
        String id = createKitchen(buildKitchenCreateBody(randomPreference));
        kitchensToCleanup.add(id);

        //No user preferences
        Response<String> response = putUserKitchenMapping(id, Collections.singletonMap("shouldNotify", true));
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Sleep till menu is processed
        Thread.sleep(10000);

        //Change user location
        response = updateUserLocation(1.0, 1.0);
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Add preferences
        response = putUserKitchenMapping(id, ImmutableMap.of(
                "shouldNotify", true,
                "preferences", Collections.singletonList(randomPreference)
                ));
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Sleep till menu is processed
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);

        //Fetch menu
        response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertEquals(1, path.getLong("totalCount"));
        assertEquals(1, path.getLong("data.size()"));
        assertEquals(id, path.getString("data[0].kitchenId"));
        assertTrue( path.getLong("data[0].items.size()") > 0);

        //Assert preference not processed
        assertNull(path.getMap("data[0].items.find { it.name == '" + randomPreference + "' }.preference"));

        double lat = path.get("data[0].lat");
        double lon = path.get("data[0].lon");

        //Change user location to menu location
        response = updateUserLocation(lat, lon);
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Sleep till user kitchen mapping is reprocessed
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);
    }

    @Test
    @Ignore //TODO: Test with global preferences
    public void testKitchenMenuChangeReprocess()  {

    }

    @Override
    protected void setup() {

    }

    @Override
    protected void tearDown() {

    }


    private Map<String, Object> buildKitchenCreateBody(String randomText) {
        return ImmutableMap.of(
                "fbPageId", FACEBOOK_PAGE_ID,
                "languages", Arrays.asList("ee-et", "ru-ru"),
                "menuSignatureText", "nothing",
                "menuConfigurations", Collections.singletonList(
                        ImmutableMap.of(
                                "type", "PLAINTEXT",
                                "value", getFixtureMenuText() + " " + randomText
                        )
                )
        );
    }

    protected Response<String> updateUserLocation(double lat, double lon) throws Exception {
        //Create User's location
        DefaultRequest request = createFrontApiSignableRequest(HttpMethodName.PUT,
                "/users/locations",
                objectMapper.writeValueAsString(ImmutableMap.of(
                        "lat", lat,
                        "lon", lon
                )),
                null,
                null);

        Response<String> response = executeSignedRequest(request);
        logAWSResponse(response);
        return response;
    }
}
