package io.kindx.backoffice.service;

import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.backoffice.dto.events.MenuCrawlEvent;
import io.kindx.backoffice.dto.events.PreferencesEvent;
import io.kindx.backoffice.dto.menu.PlainTextMenuProcessRequestDto;
import io.kindx.client.FacebookClient;
import io.kindx.constants.JanitorEventType;
import io.kindx.dao.MenuDao;
import io.kindx.dao.MenuFoodItemDao;
import io.kindx.dao.UserKitchenMappingDao;
import io.kindx.entity.MenuFoodItem;
import io.kindx.entity.UserKitchenMapping;
import io.kindx.util.IDUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.buildTestId;
import static io.kindx.util.TestUtil.mockWebDriver;
import static org.mockito.Mockito.mock;

public class PreferencesServiceTests extends BaseTests {
    private String KITCHEN_ID_2;;
    private String FACEBOOK_PAGE_ID_2 = "115449079899568";
    private String GOOGLE_PLACES_ID_2 = "ChIJrTLr-GyuEmsRBfy61i59si0";
    private PreferencesService service;


    @Before
    public void setup() {
        KITCHEN_ID_2 = buildTestId("KN00000000000001");
        super.setup();
        createAllTables();
        seedTestData();
        service = injector.getInstance(PreferencesService.class);
    }


    public void cleanup() {
        Arrays.asList(KITCHEN_ID, KITCHEN_ID_2).forEach(k -> {
            injector.getInstance(JanitorService.class)
                    .processEvent(JanitorEvent.builder()
                            .value(k)
                            .kitchenId(k)
                            .type(JanitorEventType.MENUS_BY_KITCHEN).build());
        });
    }

    @After
    public void tearDown() {
        cleanup();
        dropAllTables();
    }


    @Test
    public void testMenuPreferencesProcessing() {
        service.processPreferencesEvent(PreferencesEvent
                .builder()
                .kitchenId(KITCHEN_ID_2)
                .id(IDUtil.generatePreferencesId())
                .userId(USER_ID)
                .preferences(new HashSet<>(Arrays.asList("egusi", "akara milk", "bread")))
                .type(PreferencesEvent.Type.KITCHEN)
                .build());
        List<MenuFoodItem> items = injector.getInstance(MenuDao.class)
                .getMenusForKitchen(KITCHEN_ID_2)
                .stream()
                .map(m -> injector.getInstance(MenuFoodItemDao.class).getUserFoodItems(USER_ID, m.getMenuId()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        Assert.assertFalse(items.isEmpty());

    }

    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(FacebookClient.class).toInstance(mock(FacebookClient.class));
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
    }

    private void seedTestData() {
        cleanup();
        seedMockKitchenData();
        seedMockKitchenData(KITCHEN_ID_2, FACEBOOK_PAGE_ID_2, GOOGLE_PLACES_ID_2); //Second kitchen
        seedMockUserData(USER_ID, new HashSet<>(Arrays.asList("milk", "cheese", "chicken", "bread")));

        MenuCrawlerService crawlerService = injector.getInstance(MenuCrawlerService.class);
        MenuProcessorService processorService = injector.getInstance(MenuProcessorService.class);
        UserKitchenMappingDao mappingDao = injector.getInstance(UserKitchenMappingDao.class);

        String configSuffix = buildTestId("CFG_");

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


        String pageConfigId = "PAGE_" + configSuffix;
        String plainTextConfigId = "PLAINTEXT_" + configSuffix;
        crawlerService.processMenuCrawlEvent(MenuCrawlEvent
                .builder()
                .kitchenId(KITCHEN_ID)
                .menuConfigurationId(pageConfigId)
                .contentType(MenuCrawlEvent.ContentType.HTML)
                .url("https://google.com?i=")//uses mock driver source
                .build()
        );

        String text = "Today's Menu: \n Egusi and amala, \n Bread and Egg \n Chicken Peppersoup, \n Akara and milk";

        processorService.processPlainTextMenu(PlainTextMenuProcessRequestDto.
                builder()
                .menuConfigurationId(plainTextConfigId)
                .kitchenId(KITCHEN_ID_2)
                .text(text)
                .build());

        //Delay for ES to index
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
