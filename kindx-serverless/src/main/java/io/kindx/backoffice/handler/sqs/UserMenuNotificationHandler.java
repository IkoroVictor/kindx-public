package io.kindx.backoffice.handler.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.UserMenuNotificationEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.UserNotificationService;
import io.kindx.factory.InjectorFactory;

public class UserMenuNotificationHandler extends SqsEventHandler<UserMenuNotificationEvent> {

    private UserNotificationService userNotificationService;


    public UserMenuNotificationHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class),
                UserMenuNotificationEvent.class);
        userNotificationService = InjectorFactory.getInjector().getInstance(UserNotificationService.class);
    }


    @Override
    protected Object processEventPayload(UserMenuNotificationEvent payload) {
        userNotificationService.processUserMenuNotification(payload);
        return null;
    }

    @Override
    protected Object usagePayload(UserMenuNotificationEvent payload) {
        return payload;
    }
}
