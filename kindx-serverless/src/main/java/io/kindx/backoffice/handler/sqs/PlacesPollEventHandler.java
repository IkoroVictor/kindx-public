package io.kindx.backoffice.handler.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.PlacesPollEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.PlacesPollService;
import io.kindx.factory.InjectorFactory;

public class PlacesPollEventHandler extends SqsEventHandler<PlacesPollEvent> {

    private PlacesPollService service;

    public PlacesPollEventHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class),
                PlacesPollEvent.class);
        this.service = InjectorFactory.getInjector().getInstance(PlacesPollService.class);
    }

    @Override
    protected Object processEventPayload(PlacesPollEvent payload) {
        return service.processPollEvent(payload);
    }

    @Override
    protected Object usagePayload(PlacesPollEvent payload) {
        return payload;
    }
}
