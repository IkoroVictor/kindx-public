package io.kindx.tests.admin;

import com.google.common.collect.ImmutableMap;
import io.kindx.tests.BaseClusterTests;
import org.junit.Test;

import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class KitchenTests extends BaseClusterTests {

    private static final String FACEBOOK_PAGE_NAME = "tallinn.kitchen.demo";
    private static final String FACEBOOK_PAGE_ID = "112134816798002";
    private static final String FACEBOOK_PAGE_ID_2 = "115449079899568";
    private static final String GOOGLE_PLACES_ID = "ChIJrTLr-GyuEmsRBfy61i59si0";

    private Set<String> kitchensToCleanup = new HashSet<>();

    @Override
    protected void setup() {}

    @Test
    public void testKitchenCreate() {
        String id = given()
                .body(buildKitchenCreateBody())
                .header("Authorization", getAdminUserCredentials().getIdToken())
                .when()
                .post(getAdminUrl()+ "/kitchens")
                .then()
                .statusCode(201)
                .assertThat()
                .body("id", notNullValue())
                .body("fbPageId", equalTo(FACEBOOK_PAGE_ID))
                .body("createdTimestamp", notNullValue())
                .body("pageUrl", equalTo("https://facebook.com/" + FACEBOOK_PAGE_ID))
                .body("defaultDisplayName", equalTo("Tallinn Kitchen Demo Page"))
                .body("defaultDisplayAddress", equalTo("Nafta  55, 10176 Tallinn, Estonia"))
                .body("menuSignatureText", equalTo("nothing"))
                .body("primaryLocationSource", equalTo("FACEBOOK_PAGE"))
                .body("lineDelimiter", equalTo("\n"))
                .body("wordDelimiter", equalTo("\\s|, "))
                .body("isDisabled", equalTo(false))
                .body("menuConfigurations", notNullValue())
                .body("menuConfigurations.size()", equalTo(2))
                .body("languages", containsInAnyOrder("en-gb", "ru-ru", "ee-et"))
                .body("menuConfigurations.find { it.type == 'PAGE' }.value",
                        equalTo("http://www.gianni.ee/restoran/"))
                .body("menuConfigurations.find { it.type == 'PDF_URL' }.value",
                        equalTo("https://stenhusrestaurant.ee/wp-content/uploads/2019/10/Stenhus_menu_october2019.pdf"))
                .extract()
                .path("id");
        kitchensToCleanup.add(id);
    }

    @Test
    public void testKitchenUpdate() {
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

        given()
                .body(buildKitchenUpdateBody())
                .header("Authorization", getAdminUserCredentials().getIdToken())
                .when()
                .patch(getAdminUrl()+ "/kitchens/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("fbPageId", equalTo(FACEBOOK_PAGE_ID_2))
                .body("createdTimestamp", notNullValue())
                .body("pageUrl", equalTo("https://facebook.com/" + FACEBOOK_PAGE_ID_2))
                .body("defaultDisplayName", equalTo("Australian Cruise Group"))
                .body("defaultDisplayAddress",
                        equalTo("32 The Promenade, King Street Wharf 5, Sydney NSW 2000, Australia"))
                .body("menuSignatureText", equalTo("something"))
                .body("primaryLocationSource", equalTo("GOOGLE_PLACES"))
                .body("lineDelimiter", equalTo("\\i"))
                .body("wordDelimiter", equalTo("\\|"))
                .body("isDisabled", equalTo(false))
                .body("menuConfigurations", notNullValue())
                .body("menuConfigurations.size()", equalTo(1))
                .body("languages", containsInAnyOrder("en-gb", "sv-se"))
                .body("menuConfigurations.find { it.type == 'FACEBOOK_PAGE' }.value",
                        equalTo(FACEBOOK_PAGE_ID_2));
    }

    @Test
    public void testKitchenDelete() {
        String id = given()
                .body(buildKitchenCreateBody())
                .header("Authorization", getAdminUserCredentials().getIdToken())
                .when()
                .post(getAdminUrl()+ "/kitchens")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .header("Authorization", getAdminUserCredentials().getIdToken())
                .when()
                .delete(getAdminUrl()+ "/kitchens/" + id)
                .then()
                .statusCode(204);

        given()
                .header("Authorization", getAdminUserCredentials().getIdToken())
                .when()
                .get(getAdminUrl()+ "/kitchens/" + id)
                .then()
                .statusCode(404);

    }


    @Override
    protected void tearDown() {

        String fbId = given()
                    .header("Authorization", getAdminUserCredentials().getIdToken())
                    .when()
                    .get(getAdminUrl()+ "/kitchens/facebook/" + FACEBOOK_PAGE_ID)
                    .then().extract().path("id");

        if (fbId != null) kitchensToCleanup.add(fbId);

        fbId = given()
                    .header("Authorization", getAdminUserCredentials().getIdToken())
                    .when()
                    .get(getAdminUrl()+ "/kitchens/facebook/" + FACEBOOK_PAGE_ID_2)
                    .then().extract().path("id");
        if (fbId != null) kitchensToCleanup.add(fbId);

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
                "languages", Arrays.asList("ee-et", "ru-ru", "en-gb"),
                "menuSignatureText", "nothing",
                "menuConfigurations",Arrays.asList(
                        ImmutableMap.of(
                                "type", "PAGE",
                                "value", "http://www.gianni.ee/restoran/"
                        ),
                        ImmutableMap.of(
                                "type", "PDF_URL",
                                "value", "https://stenhusrestaurant.ee/wp-content/uploads/2019/10/Stenhus_menu_october2019.pdf"
                        )
                )
        );
    }

    private Map<String, Object> buildKitchenUpdateBody() {
        Map<String, Object> updateMap = ImmutableMap.of(
                "fbPageId", FACEBOOK_PAGE_ID_2,
                "languages", Arrays.asList("sv-se", "en-gb"),
                "menuSignatureText", "something",
                "placesId", GOOGLE_PLACES_ID,
                "menuConfigurations", Collections.singletonList(
                        ImmutableMap.of(
                                "type", "FACEBOOK_PAGE",
                                "value", FACEBOOK_PAGE_ID_2
                        )
                )
        );
        updateMap = new HashMap<>(updateMap);
        updateMap.put("primaryLocationSource", "GOOGLE_PLACES");
        updateMap.put("lineDelimiter", "\\i");
        updateMap.put("wordDelimiter", "\\|");
        return updateMap;
    }


}
