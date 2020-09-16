package io.kindx.gateway.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.user.UserLastLocation;
import io.kindx.backoffice.processor.notification.NotificationChannel;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.client.FacebookClient;
import io.kindx.dao.MenuFoodItemDao;
import io.kindx.dao.UserDao;
import io.kindx.dao.UserKitchenMappingDao;
import io.kindx.dto.GeoPointDto;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.MenuFoodItem;
import io.kindx.entity.User;
import io.kindx.entity.UserKitchenMapping;
import io.kindx.gateway.dto.UserDto;
import io.kindx.gateway.dto.UserKitchenMappingCreateDto;
import io.kindx.gateway.dto.UserUpdateDto;
import org.elasticsearch.action.get.GetResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.buildTestId;
import static io.kindx.util.TestUtil.mockWebDriver;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UserFacadeTests  extends BaseTests {

    private String userId = buildTestId("KN00000000000001");
    private UserFacade userFacade;

    private QueueService queueService = mock(QueueService.class);

    @Before
    public void setup() {
        super.setup();
        createAllTables();seedMockKitchenData();
        userFacade = injector.getInstance(UserFacade.class);
        userId = buildTestId("0000-0000-0000-0000");
        seedMockLocationData();
    }


    @Test
    public void testAddUserKitchenMappingWithAutoCreateUser() {
        userFacade.addUserKitchenMapping(userId, KITCHEN_ID, UserKitchenMappingCreateDto
                .builder()
                .shouldNotify(true).preferences(new HashSet<>(Arrays.asList("milk", "cheese", "juust")))
                .build());

        Optional<User> optional = injector.getInstance(UserDao.class).getUser(userId);
        assertTrue(optional.isPresent());

        Optional<UserKitchenMapping> mappingOptional = injector.getInstance(UserKitchenMappingDao.class)
                .getUserKitchenMapping(userId, KITCHEN_ID);

        assertTrue(mappingOptional.isPresent());
        assertEquals(3, mappingOptional.get().getFoodPreferences().size());
        assertTrue(mappingOptional.get().getFoodPreferences().contains("milk"));
        assertTrue(mappingOptional.get().getFoodPreferences().contains("cheese"));
        assertTrue(mappingOptional.get().getFoodPreferences().contains("juust"));
        assertTrue(mappingOptional.get().getShouldNotify());

        //Seed MenuFoodItem entry to test cleanup during update
        MenuFoodItemDao foodItemDao = injector.getInstance(MenuFoodItemDao.class);
        foodItemDao.forceSave(MenuFoodItem.builder()
                        .kitchenId(KITCHEN_ID)
                        .menuId("MENU0000")
                        .userId(userId)
                        .name("juust")
                        .systemName("juust")
                        .userIdName(userId + "_juust")
                        .build());
        assertEquals(1, foodItemDao.getUserFoodItems(userId, "MENU0000").size());
        //Update Mapping
        userFacade.addUserKitchenMapping(userId, KITCHEN_ID, UserKitchenMappingCreateDto
                .builder()
                .shouldNotify(false).preferences(new HashSet<>(Arrays.asList("milk", "cheese")))
                .build());

        mappingOptional = injector.getInstance(UserKitchenMappingDao.class)
                .getUserKitchenMapping(userId, KITCHEN_ID);

        assertTrue(mappingOptional.isPresent());
        assertEquals(2, mappingOptional.get().getFoodPreferences().size());
        assertTrue(mappingOptional.get().getFoodPreferences().contains("milk"));
        assertTrue(mappingOptional.get().getFoodPreferences().contains("cheese"));
        assertFalse(mappingOptional.get().getShouldNotify());

        //Assert "juust" food item cleaned up
        assertEquals(0, foodItemDao.getUserFoodItems(userId, "MENU0000").size());
    }

    @Test
    public void testRemoveUserKitchenMapping() {
        userFacade.addUserKitchenMapping(userId, KITCHEN_ID, UserKitchenMappingCreateDto
                .builder()
                .shouldNotify(true).preferences(new HashSet<>(Arrays.asList("milk", "cheese", "juust")))
                .build());

        Optional<User> userOptional = injector.getInstance(UserDao.class).getUser(userId);
        assertTrue(userOptional.isPresent());

        Optional<UserKitchenMapping> mappingOptional = injector.getInstance(UserKitchenMappingDao.class)
                .getUserKitchenMapping(userId, KITCHEN_ID);

        assertTrue(mappingOptional.isPresent());

        userFacade.deleteUserKitchenMapping(userId, KITCHEN_ID);

        userOptional = injector.getInstance(UserDao.class).getUser(userId);
        assertTrue(userOptional.isPresent());

        mappingOptional = injector.getInstance(UserKitchenMappingDao.class)
                .getUserKitchenMapping(userId, KITCHEN_ID);

        assertFalse(mappingOptional.isPresent());
    }


    @Test
    public void testAddUserKitchenMappingWithNoPreferences() {
        userFacade.addUserKitchenMapping(userId, KITCHEN_ID, UserKitchenMappingCreateDto
                .builder()
                .shouldNotify(true)
                .build());

        Optional<User> optional = injector.getInstance(UserDao.class).getUser(userId);
        assertTrue(optional.isPresent());

        Optional<UserKitchenMapping> mappingOptional = injector.getInstance(UserKitchenMappingDao.class)
                .getUserKitchenMapping(userId, KITCHEN_ID);

        assertTrue(mappingOptional.isPresent());
        assertNull( mappingOptional.get().getFoodPreferences());
        assertTrue(mappingOptional.get().getShouldNotify());
    }

    @Test
    public void testAddUserLastLocation() throws Exception {
        Double lat = Math.random();
        Double lon = Math.random();

        userFacade.addUserLastLocation(userId, GeoPointDto.builder()
                .lat(lat)
                .lon(lon)
                .build());

        GetResponse locationResponse = injector.getInstance(ElasticSearchService.class).getUserLastLocation(userId);
        assertTrue(locationResponse.isExists());

        UserLastLocation location = injector.getInstance(ObjectMapper.class)
                .readValue(locationResponse.getSourceAsBytes(), UserLastLocation.class);

        assertEquals(userId, location.getUserId());
        assertNotNull(location.getGeoPoint());
        assertEquals(lat, location.getGeoPoint().getLat());
        assertEquals(lon, location.getGeoPoint().getLon());

    }

    @Test
    public void testGetUserWithAutoCreate() {
        UserDto user = userFacade.getUser(userId);
        Optional<User> optional = injector.getInstance(UserDao.class).getUser(userId);
        assertTrue(optional.isPresent());

        assertNull(user.getGeneralFoodPreferences());
        assertEquals("en-gb", user.getLocale());
        assertEquals(NotificationChannel.NONE, user.getNotificationChannel());
        assertEquals(userId, user.getUserId());
    }

    @Test
    public void testUpdateUser() {
        userFacade.getUser(userId);
        Optional<User> optional = injector.getInstance(UserDao.class).getUser(userId);
        assertTrue(optional.isPresent());

        userFacade.updateUser(userId, UserUpdateDto
                .builder()
                .generalFoodPreferences(Collections.singleton("milk"))
                .locale("sv-se")
                .build());


        UserDto user = userFacade.getUser(userId);
        assertNotNull(user.getGeneralFoodPreferences());
        assertEquals(1, user.getGeneralFoodPreferences().size());
        assertTrue(user.getGeneralFoodPreferences().contains("milk"));
        assertEquals("sv-se", user.getLocale());
        assertEquals(NotificationChannel.NONE, user.getNotificationChannel());
        assertEquals(userId, user.getUserId());
        assertEquals(1, user.getLocations().size());
        assertNull(user.getUserLastLocation());

        verifyZeroInteractions(queueService);
    }

    @Test
    public void testUpdateUserWithAutoCreate() {
        userFacade.updateUser(userId, UserUpdateDto
                .builder()
                .generalFoodPreferences(Collections.singleton("milk"))
                .locale("sv-se")
                .build());

        UserDto user = userFacade.getUser(userId);
        assertNotNull(user.getGeneralFoodPreferences());
        assertEquals(1, user.getGeneralFoodPreferences().size());
        assertTrue(user.getGeneralFoodPreferences().contains("milk"));
        assertEquals("sv-se", user.getLocale());
        assertEquals(NotificationChannel.NONE, user.getNotificationChannel());
        assertEquals(userId, user.getUserId());
        assertEquals(1, user.getLocations().size());
        assertNull(user.getUserLastLocation());
        verifyZeroInteractions(queueService);
    }


    @Test
    public void testUpdateUserWithReprocess() {
        userFacade.getUser(userId);
        Optional<User> optional = injector.getInstance(UserDao.class).getUser(userId);
        assertTrue(optional.isPresent());

        Double lat = Math.random();
        Double lon = Math.random();

        userFacade.addUserLastLocation(userId, GeoPointDto.builder()
                .lat(lat)
                .lon(lon)
                .build());

        userFacade.updateUser(userId, UserUpdateDto
                .builder()
                .generalFoodPreferences(Collections.singleton("milk"))
                .locale("sv-se")
                .build());


        UserDto user = userFacade.getUser(userId);
        assertNotNull(user.getGeneralFoodPreferences());
        assertEquals(1, user.getGeneralFoodPreferences().size());
        assertTrue(user.getGeneralFoodPreferences().contains("milk"));
        assertEquals("sv-se", user.getLocale());
        assertEquals(NotificationChannel.NONE, user.getNotificationChannel());
        assertEquals(userId, user.getUserId());
        assertEquals(1, user.getLocations().size());
        assertNotNull(user.getUserLastLocation());
        assertEquals(lat, user.getUserLastLocation().getLat());
        assertEquals(lon, user.getUserLastLocation().getLon());

        verify(queueService, times(2)).enqueuePreferenceReprocessMessages(any());
    }


    @After
    public void tearDown() {
        dropAllTables();
        injector.getInstance(ElasticSearchService.class).deleteUserLastLocation(userId);

    }
    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(FacebookClient.class).toInstance(mock(FacebookClient.class));
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(QueueService.class).toInstance(queueService);
    }
}
