package io.kindx.backoffice.handler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.events.PlacesPollEvent;
import io.kindx.backoffice.dto.message.EventMessage;
import io.kindx.backoffice.handler.sqs.PlacesPollEventHandler;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.client.FacebookClient;
import io.kindx.util.IDUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import java.util.Collections;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.mockWebDriver;
import static org.mockito.Mockito.mock;

public class PlacesPollEventHandlerTests extends BaseTests {

    @Before
    public void setup() {
        super.setup();
        createAllTables();
        seedMockLocationData();
    }

    @After
    public void tearDown() {
        dropAllTables();
    }

    @Test
    public void testPlacesPolling() throws JsonProcessingException {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        String body = injector.getInstance(ObjectMapper.class).writeValueAsString(
                new EventMessage<>(
                        PlacesPollEvent.builder()
                                .id("1")
                                .locationId(LOCATION_ID)
                                .name("Tallinn")
                                .radiusInMeters(50)
                                .lat(59.437515)
                                .lon(24.746583)
                                .build(),
                        IDUtil.generateMessageId()
                )
        );
        message.setBody(body);
        message.setEventSource("aws.sqs");
        message.setEventSourceArn("aws:sqs:PLACES_POLL_EVENTS");
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(Collections.singletonList(message));

        new PlacesPollEventHandler()
                .handleRequest(sqsEvent, null);

    }

    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
        binder.bind(FacebookClient.class).toInstance(mock(FacebookClient.class));
    }


}
