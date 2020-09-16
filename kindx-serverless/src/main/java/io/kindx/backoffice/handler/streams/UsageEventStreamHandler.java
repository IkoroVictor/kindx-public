package io.kindx.backoffice.handler.streams;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.UsageEvent;
import io.kindx.backoffice.dto.message.EventMessage;
import io.kindx.backoffice.service.UsageEventService;
import io.kindx.factory.InjectorFactory;
import io.kindx.util.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class UsageEventStreamHandler implements RequestHandler<KinesisEvent, Integer> {

    private static final Logger logger = LogManager.getLogger(UsageEventStreamHandler.class);
    private final JavaType eventType;

    private UsageEventService eventService;
    private ObjectMapper mapper;


    public UsageEventStreamHandler() {
        this.eventService = InjectorFactory.getInjector().getInstance(UsageEventService.class);
        this.mapper = InjectorFactory.getInjector().getInstance(ObjectMapper.class);
        this.eventType = mapper.getTypeFactory().constructParametricType(EventMessage.class, UsageEvent.class);
    }

    @Override
    public Integer handleRequest(KinesisEvent input, Context context) {
        List<UsageEvent> events = new ArrayList<>();
        for (KinesisEvent.KinesisEventRecord record :input.getRecords()) {
          try {
              EventMessage<UsageEvent> event = mapper.readValue(record.getKinesis().getData().array(), eventType);
              LogUtils.setCorrelationId(context.getAwsRequestId());
              events.add(event.getPayload());
          } catch (Exception ex) {
              logger.error("Could not parse usage event for stream record {}. {}. Record: {}",
                      record.getEventID(), ex.getMessage(), record.getKinesis().getData().toString(), ex);
          }
            LogUtils.setCorrelationId(null);
        }
        if (!events.isEmpty()) {
            eventService.processUsageEvents(events);
        }
        return 0;
    }
}
