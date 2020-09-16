package io.kindx;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.google.inject.Binder;
import com.google.inject.Injector;
import io.kindx.backoffice.processor.notification.NotificationChannel;
import io.kindx.constants.Defaults;
import io.kindx.constants.Language;
import io.kindx.constants.LocationSource;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.LocationDao;
import io.kindx.dao.UserDao;
import io.kindx.dao.UserKitchenMappingDao;
import io.kindx.entity.*;
import io.kindx.factory.InjectorFactory;
import io.kindx.test.TestModule;
import io.kindx.util.DbTestUtil;
import io.kindx.util.TestUtil;
import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.*;

public abstract class BaseTests {

    protected static String KITCHEN_ID;
    protected static  String USER_ID;
    protected static String LOCATION_ID;
    protected static final String PLACES_ID = "ChIJN1t_tDeuEmsRUsoyG83frY4" ;
    protected static final String FACEBOOK_ID = "112134816798002" ;


    @ClassRule
    public static final EnvironmentVariables envVariables = TestUtil.loadTestEnvVariables("env/test.env");

    protected Injector injector;

    public void setup() {
        KITCHEN_ID = TestUtil.buildTestId("KN00000000000000");
        USER_ID = TestUtil.buildTestId("US00000000000000");
        LOCATION_ID = "LN_000000000000";

        TestModule testModule = new TestModule(this::bind);
        injector = InjectorFactory.overrideInjector(testModule);
    }


    private void bind(Binder binder){
       binder.bind(AmazonDynamoDB.class).toInstance(DbTestUtil.getDynamoDb());
       bindMocks(binder);
    }

    protected abstract void bindMocks (Binder binder);

    protected void seedMockKitchenData() {
        seedMockKitchenData(KITCHEN_ID, FACEBOOK_ID, PLACES_ID);
    }
    protected void seedMockKitchenData(String kitchenId, String fbPageId, String googlePlacesId) {
        KitchenDao dao = injector.getInstance(KitchenDao.class);
        dao.saveKitchenWithConfigurations(
                Kitchen.builder()
                        .kitchenId(kitchenId)
                        .facebookId(fbPageId)
                        .googlePlacesId(googlePlacesId)
                        .isDisabled(false)
                        .createdBy("test")
                        .createdTimestamp(new Date().getTime())
                        .build(),
                KitchenConfiguration.builder()
                        .kitchenId(kitchenId)
                        .createdTimestamp(new Date().getTime())
                        .ignoreStopWords(false)
                        .isDisabled(false)
                        .languages(new HashSet<>(Arrays.asList(
                                Language.ENGLISH,
                                Language.ESTONIAN,
                                Language.FINNISH)))
                        .lineDelimiterRegex(Defaults.LINE_DELIMITER_REGEX)
                        .wordDelimiterRegex(Defaults.WORD_DELIMITER_REGEX)
                        .primaryLocationSource(LocationSource.GOOGLE_PLACES)
                        .useBruteForceMatchIfNecessary(false)
                        .build(),
                Collections.singletonList(MenuConfiguration
                        .builder()
                        .type(MenuConfigurationType.PAGE)
                        .id("PAGE_0000000")
                        .kitchenId(kitchenId)
                        .value("http://www.gianni.ee/restoran/")
                        .createdTimeStamp(new Date().getTime())
                        .build()
                ),
                null
        );
    }

    protected void seedMockUserData() {
        seedMockUserData(USER_ID, null);
    }

    protected void seedMockUserData(String userId, Set<String> preferences) {
        UserDao userDao = injector.getInstance(UserDao.class);
        userDao.save(() -> User.builder()
                .notificationChannel(NotificationChannel.WHATSAPP)
                .locale(Defaults.USER_LOCALE)
                .isDisabled(false)
                .notificationChannelId("whatsapp:000000000")
                .generalFoodPreferences(preferences)
                .userId(userId)
                .createdTimestamp(new Date().getTime())
                .build());
    }



    protected void seedMockUserKitchenMapping(String userId,
                                              String kitchenId,
                                              Set<String> preferences) {
        UserKitchenMappingDao userKitchenMappingDao = injector.getInstance(UserKitchenMappingDao.class);
        userKitchenMappingDao.save(() -> UserKitchenMapping.builder()
                .kitchenId(kitchenId)
                .userId(userId)
                .isDisabled(false)
                .foodPreferences(preferences)
                .shouldNotify(false)
                .createdTimestamp(new Date().getTime())
                .build());
    }

    protected void seedMockLocationData() {
        injector.getInstance(LocationDao.class)
                .save(() -> Location.builder()
                        .locationId(LOCATION_ID)
                        .createdTimestamp(new Date().getTime())
                        .defaultLanguages(Collections.singleton(Language.ESTONIAN))
                        .lat(59.437515)
                        .lon(24.746583)
                        .searchRadius(10000)
                        .radiusReductionFactor(2)
                        .isDisabled(false)
                        .name("Tallinn Center").build());
    }

}
