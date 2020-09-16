package io.kindx.backoffice.service;


import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.kindx.backoffice.dto.events.MenuCrawlEvent;
import io.kindx.backoffice.dto.events.PlainTextMenuEvent;
import io.kindx.backoffice.dto.events.PreferencesEvent;
import io.kindx.backoffice.dto.events.UsageEvent;
import io.kindx.backoffice.dto.message.EventMessage;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.entity.MenuConfiguration;
import io.kindx.util.IDUtil;
import io.kindx.util.ResilienceUtil;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventService {
    private static final Logger logger = LogManager.getLogger(EventService.class);
    private static final String USAGE_EVENT_STREAM_NAME = "USAGE_EVENTS_STREAM";
    private static final String PREFERENCES_EVENTS_STREAM_NAME = "PREFERENCES_EVENTS_STREAM";

    private AmazonKinesis kinesis;
    private ObjectMapper objectMapper;
    private QueueService queueService;


    @Inject
    public EventService(AmazonKinesis kinesis,
                        ObjectMapper objectMapper,
                        QueueService queueService) {
        this.kinesis = kinesis;
        this.objectMapper = objectMapper;
        this.queueService = queueService;
    }

    public void  publishPreferencesEvent(PreferencesEvent event) {
        ResilienceUtil.retryOnExceptionSilently(() -> kinesis.putRecord(
                mapToKinesisPutRequest(PREFERENCES_EVENTS_STREAM_NAME, IDUtil.generateMessageId(), event)));
    }

    @SneakyThrows
    public void  publishPreferencesEvents(Collection<PreferencesEvent> events) {
        PutRecordsRequest recordsRequest = new PutRecordsRequest()
                .withStreamName(PREFERENCES_EVENTS_STREAM_NAME)
                .withRecords(events.stream()
                        .map(e  -> mapToKinesisPutRequestEntry(e.getId(), e))
                        .collect(Collectors.toList()));
        ResilienceUtil.retryOnExceptionSilently(() -> logger.info(
                "Preferences events published, Total: [{}],  Failed: [{}]",
                recordsRequest.getRecords().size(),
                kinesis.putRecords(recordsRequest).getFailedRecordCount()));
    }

    public void  publishUsageEvent(UsageEvent event) {
        ResilienceUtil.retryOnExceptionSilently(() -> kinesis.putRecord(
                mapToKinesisPutRequest(USAGE_EVENT_STREAM_NAME, event.getEventId(), event)));
    }

    @SneakyThrows
    public void  publishUsageEvents(Collection<UsageEvent> events) {
        PutRecordsRequest recordsRequest = new PutRecordsRequest()
                .withStreamName(USAGE_EVENT_STREAM_NAME)
                .withRecords(events.stream()
                        .map(e  -> mapToKinesisPutRequestEntry(e.getEventId(), e))
                        .collect(Collectors.toList()));
        ResilienceUtil.retryOnExceptionSilently(() -> logger.info(
                "Usage events published, Total: [{}],  Failed: [{}]",
                recordsRequest.getRecords().size(),
                kinesis.putRecords(recordsRequest).getFailedRecordCount()));
    }


    public void publishMenuEvents(String kitchenId,
                                   Collection<MenuConfiguration> configurations) {
        Map<MenuConfigurationType, List<MenuConfiguration>> grouped =
                configurations.stream().collect(Collectors.groupingBy(MenuConfiguration::getType));

        if (grouped.containsKey(MenuConfigurationType.PLAINTEXT)) {
            queueService.enqueuePlainTextMenuEventMessages(
                    grouped.get(MenuConfigurationType.PLAINTEXT).stream().map(m -> PlainTextMenuEvent
                            .builder().kitchenId(kitchenId)
                            .menuConfigurationId(m.getId()).text(m.getValue()).build())
                            .collect(Collectors.toList())
            );
        }

        if (grouped.containsKey(MenuConfigurationType.PAGE)) {
            queueService.enqueueMenuCrawlEventMessages(
                    grouped.get(MenuConfigurationType.PAGE).stream().map(m -> MenuCrawlEvent
                            .builder().kitchenId(kitchenId)
                            .menuConfigurationId(m.getId())
                            .contentType(MenuCrawlEvent.ContentType.HTML)
                            .url(m.getValue()).build())
                            .collect(Collectors.toList())
            );
        }

        if (grouped.containsKey(MenuConfigurationType.PDF_URL)) {
            queueService.enqueueMenuCrawlEventMessages(
                    grouped.get(MenuConfigurationType.PDF_URL).stream().map(m -> MenuCrawlEvent
                            .builder().kitchenId(kitchenId)
                            .menuConfigurationId(m.getId())
                            .contentType(MenuCrawlEvent.ContentType.PDF)
                            .url(m.getValue()).build())
                            .collect(Collectors.toList())
            );
        }
    }

    @SneakyThrows
    private PutRecordRequest mapToKinesisPutRequest(String streamName, String id, Object event) {
        return  new PutRecordRequest()
                .withStreamName(streamName)
                .withPartitionKey(id)
                .withData(toByteBuffer(new EventMessage<>(event, IDUtil.generateMessageId())));
    }

    @SneakyThrows
    private PutRecordsRequestEntry mapToKinesisPutRequestEntry(String id, Object event) {
        return  new PutRecordsRequestEntry()
                .withPartitionKey(id)
                .withData(toByteBuffer(new EventMessage<>(event, IDUtil.generateMessageId())));
    }


    @SneakyThrows
    private ByteBuffer toByteBuffer(Object o) {
        return ByteBuffer.wrap(objectMapper.writeValueAsBytes(o));
    }
}
