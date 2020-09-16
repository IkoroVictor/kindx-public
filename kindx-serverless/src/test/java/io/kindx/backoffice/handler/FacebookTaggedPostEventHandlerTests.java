package io.kindx.backoffice.handler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.events.FacebookTaggedPostEvent;
import io.kindx.backoffice.dto.message.EventMessage;
import io.kindx.backoffice.handler.sqs.FacebookTaggedPostEventHandler;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.client.FacebookClient;
import io.kindx.dto.facebook.FacebookPostDto;
import io.kindx.util.IDUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import java.util.Collections;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.mockFbClient;
import static io.kindx.util.TestUtil.mockWebDriver;
import static org.mockito.Mockito.*;

public class FacebookTaggedPostEventHandlerTests extends BaseTests {
    private final String FACEBOOK_PAGE_USERNAME = "tallinn.kitchen.demo";
    private final String FACEBOOK_PAGE_ID = "112134816798002";
    private final String FACEBOOK_POST_ID = "112134816798002_1000000";

    private FacebookClient facebookClient =  mockFbClient(FACEBOOK_PAGE_ID, FACEBOOK_PAGE_USERNAME, null);

    @Before
    public void setup() {
        super.setup();
        createAllTables();
        seedMockKitchenData();
    }

    @After
    public void tearDown() {
        dropAllTables();
    }

    @Test
    public void testPostPollingEventHandling() throws Exception {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        String body = injector.getInstance(ObjectMapper.class).writeValueAsString(
                new EventMessage<>(
                        FacebookTaggedPostEvent.builder()
                                .facebookId("112134816798002")
                                .kitchenId(KITCHEN_ID)
                                .post(FacebookPostDto.builder()
                                        .createdTime("2020-02-10T22:38:17+0000")
                                        .id(FACEBOOK_POST_ID)
                                        .message("Test messaging")
                                        .build())
                                .menuConfigurationId("FACEBOOK_PAGE_00000000")
                                .build(),
                        IDUtil.generateMessageId()
                )
        );
        message.setBody(body);
        message.setEventSource("aws.sqs");
        message.setEventSourceArn("aws:sqs:FACEBOOK_POLL_EVENTS");
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(Collections.singletonList(message));

        new FacebookTaggedPostEventHandler()
                .handleRequest(sqsEvent, null);

        verify(facebookClient, times(1)).getFacebookPage(
                anyString(),
                eq(FACEBOOK_PAGE_ID));
    }
    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
        binder.bind(FacebookClient.class).toInstance(facebookClient);
    }
}
