package io.kindx.backoffice.handler.schedule;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import io.kindx.backoffice.dto.events.UsageEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.constants.UsageEventSource;
import io.kindx.util.IDUtil;
import io.kindx.util.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

public abstract class ScheduleEventHandler implements RequestHandler<ScheduledEvent, Integer> {

    private static final Logger logger = LogManager.getLogger(ScheduleEventHandler.class);
    private static final String LOG_FORMAT = "[{}] [EventId: {}] Error triggering event from {} : {}";

    private EventService eventService;

    protected ScheduleEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public Integer handleRequest(ScheduledEvent input, Context context) {
        long start = currentTimeMillis();
        String correlationId = IDUtil.generateCorrelationId();
        LogUtils.setCorrelationId(correlationId);
        try {
            Object result = handleScheduledEvent(input);
            publishUsageEvent(UsageEvent.Status.SUCCESS, input, result,
                    currentTimeMillis() - start, context.getFunctionName(), correlationId);
        } catch (Exception ex) {
            logger.error(LOG_FORMAT, correlationId, input.getId(), context.getFunctionName(), ex.getMessage(), ex);
            publishUsageEvent(UsageEvent.Status.FAILED, input, null,
                    currentTimeMillis() - start, context.getFunctionName(), correlationId);
        }
        finally { LogUtils.setCorrelationId(null); }
        return 0;
    }


    protected abstract Object handleScheduledEvent(ScheduledEvent event);

    private void publishUsageEvent(UsageEvent.Status status,
                                   ScheduledEvent event,
                                   Object result,
                                   long duration,
                                   String triggerName,
                                   String correlationId) {

        Map<String, Object> meta = new HashMap<>();
        meta.put("eventId", event.getId());
        meta.put("durationInMs", duration);
        meta.put("result", result);
        meta.put("triggerSource", triggerName.toUpperCase());

        eventService.publishUsageEvent(UsageEvent.builder()
                .eventId(IDUtil.generateUsageId())
                .correlationId(correlationId)
                .createdTimestamp(new Date().getTime())
                .status(status)
                .meta(meta)
                .awsEventId(event.getId())
                .awsEventName(event.getDetailType())
                .source(UsageEventSource.SCHEDULED_EVENT)
                .build());
    }

}
