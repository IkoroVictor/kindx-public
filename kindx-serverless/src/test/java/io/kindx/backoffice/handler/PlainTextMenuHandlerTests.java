package io.kindx.backoffice.handler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.dto.events.PlainTextMenuEvent;
import io.kindx.backoffice.dto.message.EventMessage;
import io.kindx.backoffice.handler.sqs.PlainTextEventMenuHandler;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.client.FacebookClient;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.factory.InjectorFactory;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PlainTextMenuHandlerTests extends BaseTests {

    private final String FACEBOOK_PAGE_USERNAME = "tallinn.kitchen.demo";
    private final String FACEBOOK_PAGE_ID = "112134816798002";

    private FacebookClient facebookClient =  mockFbClient(FACEBOOK_PAGE_ID, FACEBOOK_PAGE_USERNAME, null);;
    private QueueService queueService =  mock(QueueService.class);
    private EventService eventService =  mock(EventService.class);

    @Before
    public void setup() {
        super.setup();
        createAllTables();
        seedMockKitchenData(KITCHEN_ID, FACEBOOK_PAGE_ID, "ChIJN1t_tDeuEmsRUsoyG83frY4");
        seedMockUserData(USER_ID, Collections.singleton("tomatikastmes"));
        seedMockUserKitchenMapping(USER_ID, KITCHEN_ID, Collections.singleton("Pasta"));
    }

    @Test
    public void testPlainTextMenuProcessing() throws JsonProcessingException {
        String menuText = "Kallisss S\u00F5ber,\r\nkutsume Sind 8.11 l\u00F5unale:\r\n\r\nBor\u0161 sealihaga 1.90\u20AC\r\n]" +
                "Lillkapsa kreemsupp 1.90\u20AC\r\n-----------------------------\r\nMeriahven tomatikastmes\r\nKana\u0161nitsel\r\n" +
                "Kana\u0161a\u0161l\u00F5kk\r\nSea\u0161a\u0161l\u00F5kk\r\nAhjukanakintsud\r\n" +
                "Praetud rohelised oad paprikaga\r\nAurutatud lillkapsas ja brokkoli\r\n" +
                "Koorene karuli-seene roog\r\nPasta k\u00F6\u00F6giviljade ja praetud munaga\r\n\r\n" +
                "Lisandid: Kartul, Riis, Tatar, Pasta\r\nSalativalik\r\n\r\n100 g = 1,09 EUR\r\n\r\nMagustoit 2.00 " +
                "\u20AC\r\nKook 2.20 \u20AC";

        EventMessage<PlainTextMenuEvent> message = new EventMessage<>(
                PlainTextMenuEvent.builder()
                        .kitchenId(KITCHEN_ID)
                        .menuConfigurationId(IDUtil.generateMenuConfigId(MenuConfigurationType.PLAINTEXT, menuText))
                        .text(menuText)
                        .build(),
                IDUtil.generateMessageId());

        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody(InjectorFactory.getInjector().getInstance(ObjectMapper.class).writeValueAsString(message));
        sqsMessage.setEventSource("aws.sqs");
        sqsMessage.setEventSourceArn("aws:sqs:PLAIN_TEXT_MENU_EVENTS");
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(Collections.singletonList(sqsMessage));

        new PlainTextEventMenuHandler()
                .handleRequest(sqsEvent, null);

        verify(eventService, times(1)).publishPreferencesEvent(any());

    }


    @After
    public void tearDown() {
        dropAllTables();
    }


    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(QueueService.class).toInstance(queueService);
        binder.bind(EventService.class).toInstance(eventService);
        binder.bind(FacebookClient.class).toInstance(facebookClient);
    }
}
