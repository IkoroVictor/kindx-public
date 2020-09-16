package io.kindx.backoffice.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.kindx.backoffice.dto.events.*;
import io.kindx.backoffice.dto.message.EventMessage;
import io.kindx.backoffice.dto.places.PolledPlacesRestaurant;
import io.kindx.dto.facebook.FacebookPageDto;
import io.kindx.dto.facebook.webhook.FacebookWebhookEventChangeDto;
import io.kindx.util.IDUtil;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class QueueService {

    private static final Logger logger = LogManager.getLogger(QueueService.class);

    private static final String UNPROCESSED_PAGE_POSTS_QUEUE_NAME = "UNPROCESSED_PAGE_POSTS";
    private static final String JANITOR_EVENTS_QUEUE_NAME = "JANITOR_EVENTS";
    private static final String MENU_CRAWL_QUEUE_NAME = "MENU_CRAWL_EVENTS";
    private static final String FACEBOOK_TAGGED_POST_QUEUE_NAME = "FACEBOOK_TAGGED_POST_EVENTS";
    private static final String FACEBOOK_WEBHOOK_CHANGES_QUEUE_NAME = "FACEBOOK_WEBHOOK_CHANGES.fifo";
    private static final String FACEBOOK_WEBHOOK_CHANGES_GRP_ID = "facebook_webhook_changes_group";
    private static final String PLACES_POLL_QUEUE_NAME = "PLACES_POLL_EVENTS";
    private static final String PLACES_TO_KITCHEN_QUEUE_NAME = "PLACES_TO_KITCHEN";
    private static final String PLAIN_TEXT_QUEUE_NAME = "PLAIN_TEXT_MENU_EVENTS";
    private static final String USER_MENU_NOTIFICATION_QUEUE_NAME = "USER_MENU_NOTIFICATIONS";
    private static final String PREFERENCE_REPROCESS_QUEUE_NAME = "PREFERENCE_REPROCESS_EVENTS";



    private AmazonSQS amazonSqs;
    private ObjectMapper objectMapper;
    private Map<String, String> queueUrls;



    @Inject
    public QueueService(AmazonSQS amazonSqs, ObjectMapper objectMapper) {
        this.amazonSqs = amazonSqs;
        this.objectMapper = objectMapper;
        this.queueUrls = new HashMap<>();
    }

    public void enqueueFacebookPagePosts (List<FacebookPageDto> pageWithPostsDtos) {
        sendBatchMessages(UNPROCESSED_PAGE_POSTS_QUEUE_NAME, pageWithPostsDtos, null);
    }

    public void enqueueJanitorEventMessages(List<JanitorEvent> janitorEvents) {
        sendBatchMessages(JANITOR_EVENTS_QUEUE_NAME, janitorEvents, null);
    }

    public void enqueueMenuCrawlEventMessages(List<MenuCrawlEvent> crawlEventDtos) {
        sendBatchMessages(MENU_CRAWL_QUEUE_NAME, crawlEventDtos, null);
    }

    public void enqueueFacebookTaggedPostEventMessages(List<FacebookTaggedPostEvent> events) {
        sendBatchMessages(FACEBOOK_TAGGED_POST_QUEUE_NAME, events, null);
    }

    public void enqueuePlainTextMenuEventMessages(List<PlainTextMenuEvent> events) {
        sendBatchMessages(PLAIN_TEXT_QUEUE_NAME, events, null);
    }

    public void enqueuePlacesPollEventMessages(List<PlacesPollEvent> events) {
        sendBatchMessages(PLACES_POLL_QUEUE_NAME, events, null);
    }

    public void enqueuePlacesToKitchenMessages(List<PolledPlacesRestaurant> restaurants) {
        sendBatchMessages(PLACES_TO_KITCHEN_QUEUE_NAME, restaurants, null);
    }

    public void enqueuePreferenceReprocessMessages(List<PreferenceReprocessEvent> events) {
        sendBatchMessages(PREFERENCE_REPROCESS_QUEUE_NAME, events, null);
    }

    public void enqueueFacebookWebhookChangesMessages(List<FacebookWebhookEventChangeDto> events) {
        sendBatchMessages(FACEBOOK_WEBHOOK_CHANGES_QUEUE_NAME, events, FACEBOOK_WEBHOOK_CHANGES_GRP_ID);
    }

    @SneakyThrows
    public void enqueueUserMessageNotification(UserMenuNotificationEvent userMenuNotification) {
        String queueUrl = getQueueUrl(USER_MENU_NOTIFICATION_QUEUE_NAME);
        SendMessageRequest request = new SendMessageRequest(queueUrl,
                objectMapper.writeValueAsString(new EventMessage<>(userMenuNotification, IDUtil.generateMessageId())));
        amazonSqs.sendMessage(request);
    }


    private <T> void sendBatchMessages(String queueName,  Collection<T> messages,
                                       String messageGroupId) {
        if (messages.isEmpty()) {
            logger.warn("Empty batch messages for {}.....ignoring....", queueName);
            return;
        }
        boolean isFifo = queueName.endsWith(".fifo");
        List<SendMessageBatchRequestEntry> batchRequestEntries = messages.
                stream()
                .map(entry -> new EventMessage<>(entry, IDUtil.generateMessageId()))
                .map(message -> mapQueueMessageToBatchEntry(message, isFifo, messageGroupId))
                .collect(Collectors.toList());
        String queueUrl = getQueueUrl(queueName);

        final int chunkSize = 10;
        final AtomicInteger counter = new AtomicInteger();

        batchRequestEntries.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                .values().stream().map(v -> new SendMessageBatchRequest().withQueueUrl(queueUrl).withEntries(v))
                .forEach(batchRequest -> amazonSqs.sendMessageBatch(batchRequest));
    }

    private String getQueueUrl(String queueName) {
        String queueUrl = queueUrls.get(queueName);
        if (queueUrl == null) {
            queueUrl = amazonSqs.getQueueUrl(queueName).getQueueUrl();
            queueUrls.put(queueName, queueUrl);
        }
        return queueUrl;
    }

    @SneakyThrows
    private SendMessageBatchRequestEntry mapQueueMessageToBatchEntry(EventMessage message, boolean isFifo, String groupId)  {
        SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry(message.getId(), objectMapper.writeValueAsString(message));
        if (isFifo) {
            entry.setMessageGroupId(groupId);
            entry.setMessageDeduplicationId(message.getId());
        }
        return entry;
    }

}
