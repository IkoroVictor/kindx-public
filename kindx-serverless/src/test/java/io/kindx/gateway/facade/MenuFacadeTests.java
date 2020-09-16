package io.kindx.gateway.facade;

import com.google.inject.Binder;
import com.google.maps.model.PlaceDetails;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.backoffice.dto.events.MenuCrawlEvent;
import io.kindx.backoffice.dto.menu.PlainTextMenuProcessRequestDto;
import io.kindx.backoffice.service.*;
import io.kindx.client.FacebookClient;
import io.kindx.client.PlacesApiClient;
import io.kindx.constants.JanitorEventType;
import io.kindx.dao.MenuDao;
import io.kindx.dao.UserKitchenMappingDao;
import io.kindx.dto.GeoPointDto;
import io.kindx.entity.Menu;
import io.kindx.entity.UserKitchenMapping;
import io.kindx.gateway.dto.*;
import io.kindx.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.buildTestId;
import static io.kindx.util.TestUtil.mockWebDriver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class MenuFacadeTests  extends BaseTests {
    private static final int MENU_COUNT = 6;
    private final String KITCHEN_ID_2 = buildTestId("KN00000000000001");
    private final String FACEBOOK_PAGE_ID_2 = "115449079899568";
    private final String PLACES_ID_2 = "ChIJrTLr-GyuEmsRBfy61i59si0";
    private MenuFacade menuFacade;
    private PlacesApiClient placesApiClient;

    @Before
    public void setup() {
        super.setup();
        createAllTables();
        seedTestData(MENU_COUNT);
        menuFacade = injector.getInstance(MenuFacade.class);
        placesApiClient = injector.getInstance(PlacesApiClient.class);
    }


    public void cleanup() {
        Arrays.asList(KITCHEN_ID, KITCHEN_ID_2).forEach(k -> {
            injector.getInstance(JanitorService.class)
                    .processEvent(JanitorEvent.builder()
                            .value(k)
                            .kitchenId(k)
                            .type(JanitorEventType.KITCHEN).build());
        });
    }

    @After
    public void tearDown() {
        cleanup();
        dropAllTables();
    }

    @Test
    public void testMenuSearch() {
        PlaceDetails placeDetails = placesApiClient.getPlaceDetails(PLACES_ID_2);

        int pageSize = 2;

        PaginatedContentDto<MenuDto> result = menuFacade.search(MenuSearchDto.builder()
                .searchString("loomaliha")
                .geoPoint(GeoPointDto.builder()
                        .lat(placeDetails.geometry.location.lat) //Distance
                        .lon(placeDetails.geometry.location.lng).build()
                )
                .pageSize(pageSize)
                .pageToken(null).build());


        assertEquals(pageSize, result.getCount().longValue());

        long resultLeft = result.getTotalCount() - pageSize;
        while (resultLeft > pageSize) {
            result = menuFacade.search(MenuSearchDto.builder()
                    .pageToken(result.getNextPageToken()).build());

            assertEquals(pageSize, result.getCount().longValue());
            assertNotNull(result.getNextPageToken());
            resultLeft = resultLeft - pageSize;
        }


        result = menuFacade.search(MenuSearchDto.builder()
                .pageToken(result.getNextPageToken()).build());

        assertEquals(resultLeft, result.getCount().longValue());
        assertNotNull(result.getNextPageToken());

        result = menuFacade.search(MenuSearchDto.builder()
                .pageToken(result.getNextPageToken()).build());

        assertEquals(0, result.getCount().longValue());

    }

    @Test
    public void testTodayMenu() {
        PlaceDetails placeDetails = placesApiClient.getPlaceDetails(PLACES_ID_2);

        int pageSize = 3;

        PaginatedContentDto<MenuDto> result = menuFacade.getTodayMenus(MenuTodayQueryDto.builder()
                .geoPoint(GeoPointDto.builder()
                        .lat(placeDetails.geometry.location.lat) //Distance
                        .lon(placeDetails.geometry.location.lng).build()
                )
                .zoneOffsetSeconds(0)
                .pageSize(pageSize)
                .pageToken(null).build(), "test");

        assertEquals(pageSize, result.getCount().longValue());

        long resultLeft = result.getTotalCount() - pageSize;
        while (resultLeft > pageSize) {
            result = menuFacade.getTodayMenus(MenuTodayQueryDto.builder()
                    .pageToken(result.getNextPageToken()).build(), "test");

            assertEquals(pageSize, result.getCount().longValue());
            assertNotNull(result.getNextPageToken());
            resultLeft = resultLeft - pageSize;
        }

        result = menuFacade.getTodayMenus(MenuTodayQueryDto.builder()
                .pageToken(result.getNextPageToken()).build(), "test");

        assertEquals(resultLeft, result.getCount().longValue());
        assertNotNull(result.getNextPageToken());

        result = menuFacade.getTodayMenus(MenuTodayQueryDto.builder()
                .pageToken(result.getNextPageToken()).build(), "test");

        assertEquals(0, result.getCount().longValue());
    }

    @Test
    public void testAllMenus() {
        PaginatedContentDto<MenuDto> result = menuFacade.getMenus(MenuQueryDto.builder().build(), null);

        assertEquals(MENU_COUNT, result.getCount().longValue());
    }


    @Test
    public void testGetSingleMenu() {
        List<Menu> menusForKitchen = injector.getInstance(MenuDao.class).getMenusForKitchen(KITCHEN_ID);
        MenuDto menu = menuFacade.getMenu(menusForKitchen.get(0).getMenuId(), KITCHEN_ID);
        assertNotNull(menu);
    }

    @Test
    public void testAllMenusForKitchen() {
        PaginatedContentDto<MenuDto> result = menuFacade.getMenus(MenuQueryDto.builder()
                .kitchenId(KITCHEN_ID_2)
                .build(), null);
        assertEquals(MENU_COUNT/2, result.getCount().longValue());
        for(MenuDto m :  result.getData()) {
            assertEquals(KITCHEN_ID_2, m.getKitchenId());
        }
    }


    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(FacebookClient.class).toInstance(mock(FacebookClient.class));
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
        binder.bind(PlacesApiClient.class).toInstance(TestUtil.mockPlacesClient(PLACES_ID, PLACES_ID_2));
    }


    private void seedTestData(int menusCount) {
        cleanup();
        seedMockKitchenData();
        seedMockKitchenData(KITCHEN_ID_2, FACEBOOK_PAGE_ID_2, PLACES_ID_2); //Second kitchen
        seedMockUserData(USER_ID, new HashSet<>(Arrays.asList("milk", "cheese", "chicken", "bread")));

        MenuCrawlerService crawlerService = injector.getInstance(MenuCrawlerService.class);
        MenuProcessorService processorService = injector.getInstance(MenuProcessorService.class);
        UserKitchenMappingDao mappingDao = injector.getInstance(UserKitchenMappingDao.class);

        String configSuffix = buildTestId("CFG_") ;

        //Seed mock user kitchen mapping
        mappingDao.save(() -> UserKitchenMapping.builder()
                .userId(USER_ID)
                .kitchenId(KITCHEN_ID)
                .isDisabled(false)
                .shouldNotify(false)
                .foodPreferences(new HashSet<>(Arrays.asList("liha", "loomaliha", "külmad vasikafileeviilud",
                        "palsamaadika oliivoliga", "piim", "juust", "salat", "mozzarella", "Lõhecarpaccio", "carpaccio"
                )))
                .createdTimestamp(new Date().getTime())
                .build());

        mappingDao.save(() -> UserKitchenMapping.builder()
                .userId(USER_ID)
                .kitchenId(KITCHEN_ID_2)
                .isDisabled(false)
                .shouldNotify(false)
                .foodPreferences(new HashSet<>(Arrays.asList("egusi", "akara milk", "bread")))
                .createdTimestamp(new Date().getTime())
                .build());

        //Seed menus
        for (int i = 0; i < menusCount/2; i++) {
            String pageConfigId  = "PAGE_" + configSuffix + i;
            String plainTextConfigId  = "PLAINTEXT_" + configSuffix + i;
            crawlerService.processMenuCrawlEvent(MenuCrawlEvent
                    .builder()
                    .kitchenId(KITCHEN_ID)
                    .menuConfigurationId(pageConfigId)
                    .contentType(MenuCrawlEvent.ContentType.HTML)
                    .url("https://google.com?i=" + i)//uses mock driver source
                    .build()
            );

            String text = "Today's Menu: \n Egusi and amala, \n Bread and Egg \n Chicken Peppersoup, \n Akara and milk";

            processorService.processPlainTextMenu(PlainTextMenuProcessRequestDto.
                    builder()
                    .menuConfigurationId(plainTextConfigId)
                    .kitchenId(KITCHEN_ID_2)
                    .text(text)
                    .build());

        }
    }
}
