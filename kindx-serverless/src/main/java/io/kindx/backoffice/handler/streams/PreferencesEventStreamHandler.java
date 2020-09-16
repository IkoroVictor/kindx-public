package io.kindx.backoffice.handler.streams;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.PreferencesEvent;
import io.kindx.backoffice.dto.events.UsageEvent;
import io.kindx.backoffice.dto.message.EventMessage;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.PreferencesService;
import io.kindx.constants.UsageEventSource;
import io.kindx.factory.InjectorFactory;
import io.kindx.util.IDUtil;
import io.kindx.util.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

public class PreferencesEventStreamHandler implements RequestHandler<KinesisEvent, Integer> {

    private static final Logger logger = LogManager.getLogger(PreferencesEventStreamHandler.class);

    private EventService eventService;
    private PreferencesService preferencesService;
    private ObjectMapper mapper;
    private JavaType eventType;

    public PreferencesEventStreamHandler() {
        this.eventService = InjectorFactory.getInjector().getInstance(EventService.class);
        this.preferencesService = InjectorFactory.getInjector().getInstance(PreferencesService.class);
        this.mapper = InjectorFactory.getInjector().getInstance(ObjectMapper.class);
        this.eventType = mapper.getTypeFactory().constructParametricType(EventMessage.class, PreferencesEvent.class);
    }

    @Override
    public Integer handleRequest(KinesisEvent input, Context context) {
        for (KinesisEvent.KinesisEventRecord record :input.getRecords()) {
            EventMessage<PreferencesEvent> event =  null;
            String correlationId = IDUtil.generateCorrelationId();
            UsageEvent.Status status = UsageEvent.Status.SUCCESS;
            try {
                event = mapper.readValue(record.getKinesis().getData().array(), eventType);
                LogUtils.setCorrelationId(correlationId);
                preferencesService.processPreferencesEvent(event.getPayload());
            } catch (Exception ex) {
                logger.error("Could not parse preferences event for stream record {}. {}. Record: {} [{}]",
                        record.getEventID(), ex.getMessage(),
                        record.getKinesis().getData().toString(), ex.getMessage(), ex);
                status = UsageEvent.Status.FAILED;
            }
            publishUsageEvent(record, status, event, correlationId);
            LogUtils.setCorrelationId(null);
        }
        return 0;
    }

    private void publishUsageEvent(KinesisEvent.KinesisEventRecord record,
                                   UsageEvent.Status status,
                                   EventMessage<PreferencesEvent> event,
                                   String correlationId) {
        eventService.publishUsageEvent(UsageEvent.builder()
                .correlationId(correlationId)
                .meta(event)
                .source(UsageEventSource.STREAM_EVENT)
                .eventId(IDUtil.generateUsageId())
                .status(status)
                .awsEventId(record.getEventID())
                .awsEventName(record.getEventName())
                .createdTimestamp(new Date().getTime())
                .build());
    }
}
