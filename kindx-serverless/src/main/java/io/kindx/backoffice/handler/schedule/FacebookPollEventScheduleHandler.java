package io.kindx.backoffice.handler.schedule;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.FacebookPollService;
import io.kindx.factory.InjectorFactory;

public class FacebookPollEventScheduleHandler extends ScheduleEventHandler {

    private FacebookPollService facebookPollService;

    public FacebookPollEventScheduleHandler() {
        super(InjectorFactory.getInjector().getInstance(EventService.class));
        this.facebookPollService = InjectorFactory.getInjector().getInstance(FacebookPollService.class);

    }

    @Override
    protected Object handleScheduledEvent(ScheduledEvent event) {
        return handlePollEvent(event);
    }

    private Object handlePollEvent(ScheduledEvent event) {
        return facebookPollService.pollFacebookTaggedPostEvents(event.getId());
    }

}
