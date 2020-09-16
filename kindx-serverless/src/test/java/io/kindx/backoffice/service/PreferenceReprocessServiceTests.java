package io.kindx.backoffice.service;

import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.client.FacebookClient;
import io.kindx.constants.JanitorEventType;
import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.mockWebDriver;
import static org.mockito.Mockito.mock;

public class PreferenceReprocessServiceTests extends BaseTests {
    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(FacebookClient.class).toInstance(mock(FacebookClient.class));
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
    }


    public void cleanup() {
        Collections.singletonList(KITCHEN_ID).forEach(k -> {
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

    }

    private void seedTestData() {
        cleanup();
        seedMockKitchenData();
        seedMockUserData(USER_ID, new HashSet<>(Arrays.asList("milk", "cheese", "chicken", "bread",
                "egusi", "akara milk",  "juust")));
    }
}
