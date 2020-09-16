package io.kindx.tests.webhooks;

import com.amazonaws.Response;
import com.google.common.collect.ImmutableMap;
import io.kindx.tests.BaseClusterTests;
import io.restassured.path.json.JsonPath;
import org.junit.Test;

import java.util.*;

import static io.kindx.tests.util.TestUtil.waitForProcessing;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

public class WebhookTests extends BaseClusterTests {

    private static final String FACEBOOK_PAGE_NAME = "tallinn.kitchen.demo";
    private static final String FACEBOOK_PAGE_ID = "112134816798002";

    private Set<String> kitchensToCleanup = new HashSet<>();

    @Override
    protected void setup() {

    }


    @Test
    public void testWebhookPageFacebookMention() throws Exception {
        String id = given()
                .body(buildKitchenCreateBody())
                .header("Authorization", getAdminUserCredentials().getIdToken())
                .when()
                .post(getAdminUrl()+ "/kitchens")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        kitchensToCleanup.add(id);

        fireFacebookWebhook("add");

        //Sleep till menu is processed
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);

        //Fetch kitchen menus
        Response<String> response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertEquals(1, path.getLong("totalCount"));
        assertEquals(1, path.getLong("data.size()"));

        //Replay webhook and assert for no duplicates
        fireFacebookWebhook("add");
        fireFacebookWebhook("add");

        //Sleep
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);

        //Fetch kitchen menus
        response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        path = JsonPath.from(response.getAwsResponse());

        //Assert no duplicates
        assertEquals(1, path.getLong("totalCount"));
        assertEquals(1, path.getLong("data.size()"));
    }

    @Test
    public void testWebhookPageFacebookMentionRemoval() throws Exception {
        String id = given()
                .body(buildKitchenCreateBody())
                .header("Authorization", getAdminUserCredentials().getIdToken())
                .when()
                .post(getAdminUrl() + "/kitchens")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        kitchensToCleanup.add(id);

        fireFacebookWebhook("add");

        //Sleep till menu is processed
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);
        //Fetch kitchen menus
        Response<String> response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertEquals(1, path.getLong("totalCount"));
        assertEquals(1, path.getLong("data.size()"));

        //Mention removal webhook
        fireFacebookWebhook("remove");

        //Sleep till menu is cleaned up
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);

        //Fetch kitchen menus
        response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        path = JsonPath.from(response.getAwsResponse());
        assertEquals(0, path.getLong("totalCount"));
        assertEquals(0, path.getLong("data.size()"));

    }

    @Test
    public void testWebhookPageFacebookMentionExecution() throws Exception {
        String id = given()
                .body(buildKitchenCreateBody())
                .header("Authorization", getAdminUserCredentials().getIdToken())
                .when()
                .post(getAdminUrl() + "/kitchens")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        kitchensToCleanup.add(id);

        fireFacebookWebhook("add");
        fireFacebookWebhook("remove");
        fireFacebookWebhook("add");
        fireFacebookWebhook("remove");

        //Sleep till menu is processed
        waitForProcessing(SLEEP_PERIOD_MILLISECONDS);
        //Fetch kitchen menus
        Response<String> response = getKitchenMenus(id);
        assertEquals(200, response.getHttpResponse().getStatusCode());

        JsonPath path = JsonPath.from(response.getAwsResponse());
        assertEquals(0, path.getLong("totalCount"));
        assertEquals(0, path.getLong("data.size()"));

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
                "fbPageId", FACEBOOK_PAGE_NAME,
                "languages", Arrays.asList("ee-et", "ru-ru"),
                "menuSignatureText", "nothing",
                "menuConfigurations", Collections.singletonList(
                        ImmutableMap.of(
                                "type", "FACEBOOK_PAGE",
                                "value", FACEBOOK_PAGE_NAME
                        )
                )
        );
    }

    private Map<String, Object> buildMentionWebhookBody(String verb) {
        return ImmutableMap.of(
                "object", "page",
                "entry", Collections.singletonList(
                        ImmutableMap.of(
                                "id", "10210299214172187",
                                "uid", "10210299214172187",
                                "time", 1520544816,
                                "changes", Collections.singletonList(
                                        ImmutableMap.of(
                                                "field", "mention",
                                                "value", ImmutableMap.of(
                                                        "item", "post",
                                                        "post_id", "112134816798002_159938142017669",
                                                        "verb", verb,
                                                        "sender_id", FACEBOOK_PAGE_ID
                                                )
                                        )
                                )
                        )
                )
        );
    }


    private void fireFacebookWebhook(String verb) {
        given()
                .body(buildMentionWebhookBody(verb))
                .when()
                .post(getWebhooksUrl()+ "/webhooks/facebook")
                .then()
                .statusCode(200);
    }
}
