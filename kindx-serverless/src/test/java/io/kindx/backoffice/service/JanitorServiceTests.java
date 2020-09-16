package io.kindx.backoffice.service;

import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.backoffice.dto.menu.PlainTextMenuProcessRequestDto;
import io.kindx.client.FacebookClient;
import io.kindx.client.PlacesApiClient;
import io.kindx.constants.JanitorEventType;
import io.kindx.dao.KitchenConfigurationDao;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.MenuDao;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.Menu;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.buildTestId;
import static io.kindx.util.TestUtil.mockPlacesClient;
import static org.mockito.Mockito.mock;

public class JanitorServiceTests extends BaseTests {

    private static final String MENU_CONFIG_ID = buildTestId("PLAINTEXT_0000000");
    private static final String MENU_ID = buildTestId("WP_00000000000000");

    private MenuProcessorService menuProcessorService;
    private JanitorService service;
    private ElasticSearchService elasticSearchService;
    private MenuDao menuDao;
    private KitchenDao kitchenDao;
    private KitchenConfigurationDao kitchenConfigurationDao;

    @Before
    public void setup() {
        super.setup();
        menuDao = injector.getInstance(MenuDao.class);
        elasticSearchService = injector.getInstance(ElasticSearchService.class);
        menuProcessorService = injector.getInstance(MenuProcessorService.class);
        service = injector.getInstance(JanitorService.class);
        kitchenDao = injector.getInstance(KitchenDao.class);
        kitchenConfigurationDao = injector.getInstance(KitchenConfigurationDao.class);
        createAllTables();
        seedTestData();
    }

    @After
    public void tearDown() {
        dropAllTables();
    }

    @Test
    public void testCleanupViaMenuConfigId() {
        List<Menu> list = menuDao.getMenusForConfigId(MENU_CONFIG_ID);
        Assert.assertEquals(1, list.size());

        Menu menu = list.get(0);
        Assert.assertEquals(KITCHEN_ID, menu.getKitchenId());
        Assert.assertEquals(MENU_CONFIG_ID, menu.getMenuConfigurationId());
        Assert.assertTrue(elasticSearchService.getMenu(menu.getMenuId()).isExists());

        service.processEvent(JanitorEvent
                .builder()
                .type(JanitorEventType.MENUS_BY_MENU_CONFIG)
                .kitchenId(KITCHEN_ID)
                .value(MENU_CONFIG_ID)
                .build());

        list = menuDao.getMenusForConfigId(MENU_CONFIG_ID);
        Assert.assertEquals(0, list.size()); //hard menu deletion (item should be deleted in db and not ES)

        Assert.assertFalse(elasticSearchService.getMenu(menu.getMenuId()).isExists());
        //TODO: Assert lines are deleted (happens  async)
        //Assert.assertFalse(elasticSearchService.getLineFromLineIndex(menu.getMenuId(),
        //                menu.getLanguages().iterator().next()).isExists());

    }


    @Test
    public void testCleanupViaKitchenId() {
        List<Menu> list = menuDao.getMenusForKitchen(KITCHEN_ID);
        Assert.assertEquals(1, list.size());

        Menu menu = list.get(0);
        Assert.assertEquals(KITCHEN_ID, menu.getKitchenId());
        Assert.assertEquals(MENU_CONFIG_ID, menu.getMenuConfigurationId());
        Assert.assertTrue(elasticSearchService.getMenu(menu.getMenuId()).isExists());

        service.processEvent(JanitorEvent
                .builder()
                .type(JanitorEventType.MENUS_BY_KITCHEN)
                .kitchenId(KITCHEN_ID)
                .value(KITCHEN_ID)
                .build());

        list = menuDao.getMenusForKitchen(KITCHEN_ID);
        Assert.assertEquals(0, list.size()); //hard menu deletion (item should be deleted in db and not ES)

        Assert.assertFalse(elasticSearchService.getMenu(menu.getMenuId()).isExists());
        //TODO: Assert lines are deleted (happens  async making tests unstable)
        //Assert.assertFalse(elasticSearchService.getLineFromLineIndex(menu.getMenuId(),
        //                menu.getLanguages().iterator().next()).isExists());

    }


    @Test
    public void testCleanupKitchen() {
        List<Menu> list = menuDao.getMenusForKitchen(KITCHEN_ID);
        Assert.assertEquals(1, list.size());

        Menu menu = list.get(0);
        Assert.assertEquals(KITCHEN_ID, menu.getKitchenId());
        Assert.assertEquals(MENU_CONFIG_ID, menu.getMenuConfigurationId());
        Assert.assertTrue(elasticSearchService.getMenu(menu.getMenuId()).isExists());

        service.processEvent(JanitorEvent
                .builder()
                .type(JanitorEventType.KITCHEN)
                .kitchenId(KITCHEN_ID)
                .value(KITCHEN_ID)
                .build());

        list = menuDao.getMenusForKitchen(KITCHEN_ID);
        Assert.assertEquals(0, list.size()); //hard menu deletion (item should be deleted in db and not ES)
        Assert.assertFalse( kitchenDao.getKitchenByKitchenId(KITCHEN_ID).isPresent());
        Assert.assertFalse( kitchenConfigurationDao.getKitchenConfiguration(KITCHEN_ID, false).isPresent());
        Assert.assertFalse(elasticSearchService.getMenu(menu.getMenuId()).isExists());
    }

    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
        binder.bind(FacebookClient.class).toInstance(mock(FacebookClient.class));
        binder.bind(PlacesApiClient.class).toInstance(mockPlacesClient(PLACES_ID));
    }

    private void seedTestData() {
        seedMockKitchenData();
        String text = "This is milk or in finnish (maito) or estonian (piim). I like cheese or juust";
        menuProcessorService.processPlainTextMenu(PlainTextMenuProcessRequestDto.
                builder()
                .menuConfigurationId(MENU_CONFIG_ID)
                .kitchenId(KITCHEN_ID)
                .text(text)
                .build());
    }

}
