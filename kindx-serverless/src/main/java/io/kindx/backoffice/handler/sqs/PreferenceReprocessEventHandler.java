package io.kindx.backoffice.handler.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.PreferenceReprocessEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.PreferenceReprocessService;
import io.kindx.factory.InjectorFactory;

public class PreferenceReprocessEventHandler extends SqsEventHandler<PreferenceReprocessEvent> {

    private final PreferenceReprocessService processorService;

    public PreferenceReprocessEventHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class),
                PreferenceReprocessEvent.class);
        this.processorService = InjectorFactory.getInjector().getInstance(PreferenceReprocessService.class);
    }

    @Override
    protected Object processEventPayload(PreferenceReprocessEvent payload) {
        return processorService.handleReprocessEvent(payload);
    }

    @Override
    protected Object usagePayload(PreferenceReprocessEvent payload) {
        return payload;
    }
}
