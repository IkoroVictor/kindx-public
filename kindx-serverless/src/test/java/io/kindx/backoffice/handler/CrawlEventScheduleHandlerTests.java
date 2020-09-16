package io.kindx.backoffice.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.handler.schedule.CrawlEventScheduleHandler;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.client.FacebookClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.mockWebDriver;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CrawlEventScheduleHandlerTests extends BaseTests {

    private QueueService queueService = mock(QueueService.class);
    private Context context = mock(Context.class);

    @Before
    public void setup() {
        super.setup();
        System.setProperty("webdriver.gecko.driver", "./geckodriver");
        createAllTables();
        seedMockKitchenData();
        when(context.getFunctionName()).thenReturn("CrawlTrigger");

    }

    @After
    public void tearDown() {
        dropAllTables();
    }

    @Test
    public void testCrawlEventSchedule() {
        new CrawlEventScheduleHandler().handleRequest(new ScheduledEvent().withId("1"), context);

        verify(queueService, times(1)).enqueueMenuCrawlEventMessages(any());
    }

    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(QueueService.class).toInstance(queueService);
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(FacebookClient.class).toInstance(mock(FacebookClient.class));
    }
}
