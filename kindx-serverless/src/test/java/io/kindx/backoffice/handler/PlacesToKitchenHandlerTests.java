package io.kindx.backoffice.handler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.message.EventMessage;
import io.kindx.backoffice.dto.places.PolledPlacesRestaurant;
import io.kindx.backoffice.handler.sqs.PlacesToKitchenHandler;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.client.FacebookClient;
import io.kindx.constants.Language;
import io.kindx.dto.GeoPointDto;
import io.kindx.util.IDUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import java.util.Collections;
import java.util.Date;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.mockWebDriver;
import static org.mockito.Mockito.mock;

public class PlacesToKitchenHandlerTests extends BaseTests {

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
    public void testPlacesToKitchenHandler() throws Exception {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        String body = injector.getInstance(ObjectMapper.class).writeValueAsString(
                new EventMessage<>(
                        PolledPlacesRestaurant
                                .builder()
                                .name("Test kitchen")
                                .placesId("Ch0000000000000000")
                                .locationId("LN_0000000000000000")
                                .address("Test Address")
                                .placeUrl("http://local.url")
                                .website("http://web.url")
                                .defaultLanguages(Collections.singleton(Language.ENGLISH))
                                .internationalPhone("+2344567788")
                                .menuPageUrl("http://menu.url")
                                .pdfUrl("http://menu.pdf.url")
                                .phone("04567788")
                                .facebookPageId(null)
                                .type("cafe")
                                .geoPoint(new GeoPointDto())
                                .createdTimestamp(new Date().getTime())
                                .build(),
                        IDUtil.generateMessageId()
                )
        );
        message.setBody(body);
        message.setEventSource("aws.sqs");
        message.setEventSourceArn("aws:sqs:PLACES_TO_KITCHEN");
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(Collections.singletonList(message));

        new PlacesToKitchenHandler()
                .handleRequest(sqsEvent, null);
    }


    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(FacebookClient.class).toInstance(mock(FacebookClient.class));
    }
}
