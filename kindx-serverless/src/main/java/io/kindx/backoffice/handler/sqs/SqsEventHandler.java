package io.kindx.backoffice.handler.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.UsageEvent;
import io.kindx.backoffice.dto.events.UsageEvent.Status;
import io.kindx.backoffice.dto.message.EventMessage;
import io.kindx.backoffice.service.EventService;
import io.kindx.constants.UsageEventSource;
import io.kindx.util.IDUtil;
import io.kindx.util.LogUtils;
import io.kindx.util.ResilienceUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static io.kindx.backoffice.dto.events.UsageEvent.Status.FAILED;
import static io.kindx.backoffice.dto.events.UsageEvent.Status.SUCCESS;
import static io.kindx.metrics.MetricsLogger.logMetrics;
import static java.lang.System.currentTimeMillis;

public abstract class SqsEventHandler<T> implements RequestHandler<SQSEvent, Integer> {
    private static final Logger logger = LogManager.getLogger(SqsEventHandler.class);

    protected final ObjectMapper mapper;
    private EventService eventService;
    private JavaType queueMessageType;

    protected SqsEventHandler(ObjectMapper mapper,
                              EventService eventService,
                              Class<T> payloadClass) {
        this.mapper = mapper;
        this.eventService = eventService;
        this.queueMessageType = mapper.getTypeFactory().constructParametricType(EventMessage.class, payloadClass);
    }

    @Override
    public Integer handleRequest(SQSEvent input, Context context) {
        for (SQSEvent.SQSMessage sqsMessage : input.getRecords()) {
            long start = currentTimeMillis();
            EventMessage<T> queueMessage = null;
            String correlationId = null;
            try {
                queueMessage = mapper.readValue(sqsMessage.getBody(), queueMessageType);
                correlationId = queueMessage.getCorrelationId();
                LogUtils.setCorrelationId(correlationId);
                T payload = queueMessage.getPayload();
                logger.info("Processing '{}' queue message '{}' with event message '{}'",
                        payload.getClass().getSimpleName().toUpperCase(),
                        queueMessage.getId(),
                        sqsMessage.getMessageId());

                Object[] result  = new Object[1];
                EventMessage<T> finalQueueMessage = queueMessage;
                ResilienceUtil.retryOnException(() ->  result[0] = processEventPayload(finalQueueMessage.getPayload()));

                sendUsageEvent(SUCCESS, sqsMessage, queueMessage, result[0], currentTimeMillis() - start);
                logger.info("Done processing queue message '{}'", queueMessage.getId());
            } catch (Exception ex) {
                //TODO: SEND FAILURE EVENTS
                logger.error("[{}] - Could not process [Event Message: '{}']. Reason: {}",
                        correlationId,
                        sqsMessage.getMessageId(),
                        ex.getMessage(), ex);
                sendUsageEvent(FAILED, sqsMessage, queueMessage, null, currentTimeMillis() - start);
                throw new RuntimeException(ex);
            }
            LogUtils.setCorrelationId(null);
        }
        return 0;
    }

    protected abstract Object processEventPayload(T payload);
    protected abstract Object usagePayload(T payload);


    protected void sendUsageEvent(Status status,
                                  SQSEvent.SQSMessage sqsMessage,
                                  EventMessage<T> queueMessage,
                                  Object result,
                                  long duration) {

        String[] splitArn  = sqsMessage.getEventSourceArn().split(":");
        String queueName =  splitArn[splitArn.length - 1].toUpperCase();
        Map<String, Object> meta = new HashMap<>();
        meta.put("sqsMessageId", sqsMessage.getMessageId());
        meta.put("queueName", queueName);
        meta.put("durationInMs", duration);
        meta.put("result", result);

        String correlationId =  null;
        if (queueMessage != null) {
            correlationId =  queueMessage.getCorrelationId();
            meta.put("queueMessageId", queueMessage.getId());
            meta.put("queueMessageTimestamp", queueMessage.getMessageTimestamp().toEpochSecond() * 1000 );
            meta.put("name", queueMessage.getPayload().getClass().getSimpleName().toUpperCase());
            meta.put("payload", usagePayload(queueMessage.getPayload()));
        }
        logMetrics(this.getClass(), duration, queueName, Collections.singletonMap("status", status.name()));
        eventService.publishUsageEvent(UsageEvent.builder()
                .eventId(IDUtil.generateUsageId())
                .correlationId(correlationId)
                .createdTimestamp(new Date().getTime())
                .status(status)
                .meta(meta)
                .awsEventId(sqsMessage.getMessageId())
                .awsEventName(sqsMessage.getEventSource())
                .source(UsageEventSource.QUEUE_MESSAGE)
                .build());

    }


}
