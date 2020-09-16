package io.kindx.tests.front;

import com.amazonaws.Response;
import com.google.common.collect.ImmutableMap;
import io.kindx.tests.BaseClusterTests;
import io.restassured.path.json.JsonPath;
import org.junit.Test;

import java.util.*;

import static io.kindx.tests.util.TestUtil.waitForProcessing;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class UserMappingTests extends BaseClusterTests {

    private static final String FACEBOOK_PAGE_ID = "112134816798002";
    private Set<String> kitchensToCleanup = new HashSet<>();

    @Override
    protected void setup() { }

    @Test
    public void testUserKitchenMappingCreate() throws Exception {
        String id = createKitchen(buildKitchenCreateBody());
        kitchensToCleanup.add(id);

        Response<String> response = putUserKitchenMapping(id, buildUserMappingCreateBody());
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Sleep till menu is processed
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);

        response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertEquals(1, path.getLong("totalCount"));
        assertEquals(1, path.getLong("data.size()"));
        assertEquals(id, path.getString("data[0].kitchenId"));
        assertEquals("Tallinn Kitchen Demo Page", path.getString("data[0].kitchenName"));
        assertEquals(getFixtureMenuText(), path.getString("data[0].menu"));
        assertEquals("PLAINTEXT", path.getString("data[0].source"));
        assertEquals("Nafta  55, 10176 Tallinn, Estonia", path.getString("data[0].address"));
        assertEquals("Nafta  55, Tallinn, Estonia, 10176", path.getString("data[0].fullAddress"));
        assertEquals("http://facebook.com/" + FACEBOOK_PAGE_ID, path.getString("data[0].pageUrl"));
        assertEquals(1, path.getLong("data[0].emails.size()"));
        assertEquals("victorbenziko@gmail.com", path.getString("data[0].emails[0]"));
        assertEquals(1, path.getLong("data[0].phones.size()"));
        assertEquals("+37254000000", path.getString("data[0].phones[0]"));
        assertEquals(2, path.getLong("data[0].languages.size()"));
        assertEquals("en-gb", path.getString("data[0].languages.find { it == 'en-gb' }"));
        assertEquals("ee-et", path.getString("data[0].languages.find { it == 'ee-et' }"));
        assertNotNull(path.getString("data[0].postedTimestamp"));
        assertNotNull(path.getString("data[0].lat"));
        assertNotNull(path.getString("data[0].lon"));
        assertNotNull(path.getString("data[0].menuId"));
        assertNotNull(path.getString("data[0].thumbnailUrl"));
        assertNotNull(path.getString("data[0].headerUrl"));

        assertTrue( path.getLong("data[0].items.size()") > 0);
        assertTrue(path.getBoolean("data[0].items.find { it.name == 'schnitzel' }.preference"));
        assertTrue(path.getBoolean("data[0].items.find { it.name == 'Guljass' }.preference"));

    }


    @Test
    public void testUserKitchenMappingCreateNoPreferences() throws Exception {
        String id = createKitchen(buildKitchenCreateBody());
        kitchensToCleanup.add(id);

        Response<String> response = putUserKitchenMapping(id, buildUserMappingCreateBody(null));
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Fetch mapping
        response = getUserKitchenMapping(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertTrue(path.getBoolean("shouldNotify"));
        assertNull(path.getList("preferences"));
    }


    @Test
    public void testUserKitchenMappingFetch() throws Exception {
        String id = createKitchen(buildKitchenCreateBody());
        kitchensToCleanup.add(id);

        Response<String> response = putUserKitchenMapping(id, buildUserMappingCreateBody(Arrays.asList("test", "test2")));
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Fetch mapping
        response = getUserKitchenMapping(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertTrue(path.getBoolean("shouldNotify"));
        assertNotNull(path.getList("preferences"));
        assertEquals(2, path.getLong("preferences.size()"));
        assertNotNull(path.getString("preferences.find { it == 'test' }"));
        assertNotNull(path.getString("preferences.find { it == 'test2' }"));
    }

    @Test
    public void testUserKitchenMappingDelete() throws Exception {
        String id = createKitchen(buildKitchenCreateBody());
        kitchensToCleanup.add(id);

        Response<String> response = putUserKitchenMapping(id, buildUserMappingCreateBody());
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Sleep till menu is processed
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);


        //Fetch kitchen menus
        response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertEquals(1, path.getLong("totalCount"));
        assertEquals(1, path.getLong("data.size()"));
        assertEquals(id, path.getString("data[0].kitchenId"));
        assertTrue( path.getLong("data[0].items.size()") > 0);
        assertTrue(path.getBoolean("data[0].items.find { it.name == 'schnitzel' }.preference"));
        assertTrue(path.getBoolean("data[0].items.find { it.name == 'Guljass' }.preference"));

        response = deleteUserKitchenMapping(id);
        assertEquals(204, response.getHttpResponse().getStatusCode());


        //Fetch kitchen menus
        response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        path = JsonPath.from(response.getAwsResponse());
        assertEquals(1, path.getLong("totalCount"));
        assertEquals(1, path.getLong("data.size()"));
        assertEquals(id, path.getString("data[0].kitchenId"));
        assertTrue( path.getLong("data[0].items.size()") > 0);

        //No longer a preference, but still in food items aggregation
        assertFalse(path.getBoolean("data[0].items.find { it.name == 'schnitzel' }.preference"));
        assertFalse(path.getBoolean("data[0].items.find { it.name == 'Guljass' }.preference"));


        //Fetch mapping
        response = getUserKitchenMapping(id);
        assertEquals(404, response.getHttpResponse().getStatusCode());

    }

    @Test
    public void testUserKitchenMappingUpdate() throws Exception {
        //Create Kitchen
        String id = createKitchen(buildKitchenCreateBody());
        kitchensToCleanup.add(id);

        //Create user mapping
        Response<String> response = putUserKitchenMapping(id, buildUserMappingCreateBody());
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Sleep till menu is processed
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);


        //Fetch kitchen menus
        response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());


        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertEquals(1, path.getLong("totalCount"));
        assertEquals(1, path.getLong("data.size()"));
        assertEquals(id, path.getString("data[0].kitchenId"));
        assertTrue( path.getLong("data[0].items.size()") > 0);
        assertTrue(path.getBoolean("data[0].items.find { it.name == 'schnitzel' }.preference"));
        assertTrue(path.getBoolean("data[0].items.find { it.name == 'Guljass' }.preference"));

        //Update user kitchen mapping
        response = putUserKitchenMapping(id, buildUserMappingUpdateBody());
        assertEquals(202, response.getHttpResponse().getStatusCode());

        //Sleep till menu is re-processed with new mapping
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);

        //Fetch menu again
        response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        path = JsonPath.from(response.getAwsResponse());
        assertEquals(1, path.getLong("totalCount"));
        assertEquals(1, path.getLong("data.size()"));
        assertEquals(id, path.getString("data[0].kitchenId"));
        assertTrue( path.getLong("data[0].items.size()") > 0);

        assertFalse(path.getBoolean("data[0].items.find { it.name == 'schnitzel' }.preference"));

        assertTrue(path.getBoolean("data[0].items.find { it.name == 'Guljass' }.preference"));
        assertTrue(path.getBoolean("data[0].items.find { it.name == 'Kanašnitsel' }.preference"));
        assertTrue(path.getBoolean("data[0].items.find { it.name == 'tomato sauce' }.preference"));
        assertTrue(path.getBoolean("data[0].items.find { it.name == 'hernesupp' }.preference"));
    }

    @Override
    protected void tearDown() {
        for (String id : kitchensToCleanup) {
            given()
                    .header("Authorization", getAdminUserCredentials().getIdToken())
                    .when()
                    .delete(getAdminUrl()+ "/kitchens/" + id)
                    .then()
                    .statusCode(204);
        }

    }


    private Map<String, Object> buildKitchenCreateBody() {
        return ImmutableMap.of(
                "fbPageId", FACEBOOK_PAGE_ID,
                "languages", Arrays.asList("ee-et", "en-gb"),
                "menuSignatureText", "nothing",
                "menuConfigurations", Collections.singletonList(
                        ImmutableMap.of(
                                "type", "PLAINTEXT",
                                "value", getFixtureMenuText()
                        )
                )
        );
    }

    private Map<String, Object> buildUserMappingCreateBody() {
        return buildUserMappingCreateBody(Arrays.asList(
                        "schnitzel",
                        "kana",
                        "Guljass",
                        "supp",
                        "Juust",
                        "püree",
                        "piim"
                ));
    }

    private Map<String, Object> buildUserMappingCreateBody(Collection<String> preferences) {
        Map<String, Object> body = new HashMap<>();
        body.put("preferences", preferences);
        body.put("shouldNotify", true);
        return body;
    }

    private Map<String, Object> buildUserMappingUpdateBody() {
        return ImmutableMap.of(
                "preferences", Arrays.asList(
                        "hernesupp",
                        "tomato sauce",
                        "Kanašnitsel",
                        "Guljass",
                        "Juust",
                        "püree",
                        "piim"
                ),
                "shouldNotify", false
        );
    }





}
