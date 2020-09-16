package io.kindx.backoffice.handler.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.JanitorService;
import io.kindx.factory.InjectorFactory;

public class JanitorEventHandler extends SqsEventHandler<JanitorEvent> {

    private JanitorService janitorService;

    public JanitorEventHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class),
                JanitorEvent.class);
        this.janitorService = InjectorFactory.getInjector().getInstance(JanitorService.class);
    }

    @Override
    protected Object processEventPayload(JanitorEvent payload) {
        janitorService.processEvent(payload);
        return null;

    }

    @Override
    protected Object usagePayload(JanitorEvent payload) {
        return payload;
    }

}
