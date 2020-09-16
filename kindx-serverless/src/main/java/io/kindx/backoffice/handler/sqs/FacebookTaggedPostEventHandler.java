package io.kindx.backoffice.handler.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.FacebookTaggedPostEvent;
import io.kindx.backoffice.dto.menu.FacebookPageProcessRequestDto;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.MenuProcessorService;
import io.kindx.factory.InjectorFactory;

public class FacebookTaggedPostEventHandler extends SqsEventHandler<FacebookTaggedPostEvent> {

    private MenuProcessorService processorService;

    public FacebookTaggedPostEventHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class),
                FacebookTaggedPostEvent.class);
        this.processorService = InjectorFactory.getInjector().getInstance(MenuProcessorService.class);
    }

    @Override
    protected Object processEventPayload(FacebookTaggedPostEvent payload) {
        return processorService.processFacebookPage(FacebookPageProcessRequestDto
                .builder()
                .kitchenId(payload.getKitchenId())
                .post(payload.getPost())
                .menuConfigurationId(payload.getMenuConfigurationId())
                .pageId(payload.getFacebookId()).build());
    }

    @Override
    protected Object usagePayload(FacebookTaggedPostEvent payload) {
        return payload;
    }
}
