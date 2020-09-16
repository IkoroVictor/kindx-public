package io.kindx.backoffice.service;


import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.events.MenuCrawlEvent;
import io.kindx.backoffice.exception.CrawlerException;
import io.kindx.client.FacebookClient;
import io.kindx.constants.Defaults;
import io.kindx.constants.MenuSource;
import io.kindx.dao.MenuDao;
import io.kindx.entity.Menu;
import io.kindx.util.EnvUtil;
import io.kindx.util.FirefoxReaderModeWebDriverDelegate;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.List;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.mockWebDriver;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MenuCrawlServiceTests extends BaseTests {

    private WebDriver driver;

    @Before
    public void setup() {
        super.setup();
        System.setProperty("webdriver.gecko.driver", "./geckodriver");
        createAllTables();
        seedMockKitchenData();
    }

    @After
    public void tearDown() {
        dropAllTables();
    }


    @Test
    public void testCrawlingHtml() {
        injector.getInstance(MenuCrawlerService.class).processMenuCrawlEvent(MenuCrawlEvent
                .builder()
                .kitchenId(KITCHEN_ID)
                .menuConfigurationId("PAGE_00000000")
                .contentType(MenuCrawlEvent.ContentType.HTML)
                .url("http://www.gianni.ee/restoran/")
                .build()
        );

        List<Menu> menus = injector.getInstance(MenuDao.class).getMenusForKitchen(KITCHEN_ID);
        Assert.assertEquals(1, menus.size());

        Menu menu = menus.get(0);
        Assert.assertEquals(MenuSource.WEBPAGE, menu.getSource());
        Assert.assertNotNull(menu.getSourceUrl());
        Assert.assertEquals("http://www.gianni.ee/restoran/", menu.getSourceUrl());

        Assert.assertNull(menu.getSourceValue());//Should not be saved in db, only in ES
        Assert.assertEquals(KITCHEN_ID, menu.getKitchenId());
        Assert.assertNotNull(menu.getMenuId());
        Assert.assertNull(menu.getMenuText()); //Only save in ES
        Assert.assertNotNull(menu.getLanguages());
        Assert.assertEquals(3, menu.getLanguages().size());
        Assert.assertNotNull(menu.getLocation());
        Assert.assertNotNull(menu.getLocation().getName());
        Assert.assertNotNull(menu.getLocation().getAddress());
        Assert.assertNotNull(menu.getLocation().getCountry());
        Assert.assertNotNull(menu.getLocation().getGeoPoint());
        Assert.assertNotNull(menu.getBusinessProfile());
        Assert.assertNotNull(menu.getBusinessProfile().getBusinessName());
        Assert.assertNotNull(menu.getBusinessProfile().getOpeningHours());
        Assert.assertNotNull(menu.getBusinessProfile().getWebsite());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation().getName());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation().getAddress());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation().getCountry());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation().getGeoPoint());

    }

    @Test
    public void testCrawlingPdf() {
        String url = "https://stenhusrestaurant.ee/wp-content/uploads/2019/10/Stenhus_menu_october2019.pdf";
        injector.getInstance(MenuCrawlerService.class).processMenuCrawlEvent(MenuCrawlEvent
                .builder()
                .kitchenId(KITCHEN_ID)
                .menuConfigurationId("PDF_00000000")
                .contentType(MenuCrawlEvent.ContentType.PDF)
                .url(url)
                .build()
        );

        List<Menu> menus = injector.getInstance(MenuDao.class).getMenusForKitchen(KITCHEN_ID);
        Assert.assertEquals(1, menus.size());

        Menu menu = menus.get(0);
        Assert.assertEquals(MenuSource.PDF, menu.getSource());
        Assert.assertNotNull(menu.getSourceUrl());
        Assert.assertEquals(url, menu.getSourceUrl());

        Assert.assertNull(menu.getSourceValue());//Should not be saved in db, only in ES
        Assert.assertEquals(KITCHEN_ID, menu.getKitchenId());
        Assert.assertNotNull(menu.getMenuId());
        Assert.assertNull(menu.getMenuText()); //Only save in ES
        Assert.assertNotNull(menu.getLanguages());
        Assert.assertEquals(3, menu.getLanguages().size());
        Assert.assertNotNull(menu.getLocation());
        Assert.assertNotNull(menu.getLocation().getName());
        Assert.assertNotNull(menu.getLocation().getAddress());
        Assert.assertNotNull(menu.getLocation().getCountry());
        Assert.assertNotNull(menu.getLocation().getGeoPoint());
        Assert.assertNotNull(menu.getBusinessProfile());
        Assert.assertNotNull(menu.getBusinessProfile().getBusinessName());
        Assert.assertNotNull(menu.getBusinessProfile().getOpeningHours());
        Assert.assertNotNull(menu.getBusinessProfile().getWebsite());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation().getName());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation().getAddress());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation().getCountry());
        Assert.assertNotNull(menu.getBusinessProfile().getLocation().getGeoPoint());

    }

    @Test(expected = CrawlerException.class)
    public void testFailedCrawlingHtml() {
        mockFailedDriverResponse();
        injector.getInstance(MenuCrawlerService.class).processMenuCrawlEvent(MenuCrawlEvent
                .builder()
                .kitchenId(KITCHEN_ID)
                .menuConfigurationId("PAGE_00000000")
                .contentType(MenuCrawlEvent.ContentType.HTML)
                .url("http://goal.com")
                .build()
        );}

    protected void bindMocks(Binder binder) {
        if (StringUtils.isBlank(EnvUtil.getEnv("TEST_REAL_WEBDRIVER"))) {
            driver = mockWebDriver();
        } else {
            long browserTimeout = EnvUtil.getEnvLongOrDefault("BROWSER_LOAD_TIMEOUT_SECONDS",
                    Defaults.BROWSER_LOAD_TIMEOUT_SECONDS);
            driver = new FirefoxDriver();
            driver.manage().timeouts().implicitlyWait(browserTimeout, SECONDS);
            driver = new FirefoxReaderModeWebDriverDelegate(driver, 10L);
        }

        binder.bind(WebDriver.class).toInstance(driver);
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(FacebookClient.class).toInstance(mock(FacebookClient.class));
    }

    private void mockFailedDriverResponse() {
        if (StringUtils.isBlank(EnvUtil.getEnv("TEST_REAL_WEBDRIVER"))) {
            when(driver.getPageSource()).thenThrow(new WebDriverException("Reader mode failure exception "));
        }
    }
}
