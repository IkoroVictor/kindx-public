package io.kindx.backoffice.handler.schedule;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.PlacesToKitchenService;
import io.kindx.factory.InjectorFactory;

public class PlacesToKitchenEventScheduler extends ScheduleEventHandler {

    private final PlacesToKitchenService service;

    public PlacesToKitchenEventScheduler() {
        super(InjectorFactory.getInjector().getInstance(EventService.class));
        this.service = InjectorFactory.getInjector().getInstance(PlacesToKitchenService.class);
    }

    @Override
    protected Object handleScheduledEvent(ScheduledEvent event) {
        return service.scheduleValidatedForProcessing();
    }
}
