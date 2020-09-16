package io.kindx.backoffice.handler.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.FacebookWebhookService;
import io.kindx.dto.facebook.webhook.FacebookWebhookEventChangeDto;
import io.kindx.factory.InjectorFactory;

public class FacebookWebhookChangeEventHandler extends SqsEventHandler<FacebookWebhookEventChangeDto> {

    private FacebookWebhookService webhookService;

    public FacebookWebhookChangeEventHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class),
                FacebookWebhookEventChangeDto.class);
        webhookService = InjectorFactory.getInjector().getInstance(FacebookWebhookService.class);
    }

    @Override
    protected Object processEventPayload(FacebookWebhookEventChangeDto payload) {
        return webhookService.processChange(payload);
    }

    @Override
    protected Object usagePayload(FacebookWebhookEventChangeDto payload) {
        return payload;
    }
}
