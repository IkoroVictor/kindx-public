package io.kindx.backoffice.handler.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.PlainTextMenuEvent;
import io.kindx.backoffice.dto.menu.PlainTextMenuProcessRequestDto;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.MenuProcessorService;
import io.kindx.factory.InjectorFactory;

public class PlainTextEventMenuHandler extends SqsEventHandler<PlainTextMenuEvent> {

    private MenuProcessorService processorService;

    public PlainTextEventMenuHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class),
                PlainTextMenuEvent.class);
        this.processorService = InjectorFactory.getInjector().getInstance(MenuProcessorService.class);
    }

    @Override
    protected Object processEventPayload(PlainTextMenuEvent payload) {
        processorService.processPlainTextMenu(PlainTextMenuProcessRequestDto
                .builder()
                .kitchenId(payload.getKitchenId())
                .menuConfigurationId(payload.getMenuConfigurationId())
                .text(payload.getText()).build());
        return null;
    }

    @Override
    protected Object usagePayload(PlainTextMenuEvent payload) {
        //Exempt plain text value
        return PlainTextMenuEvent
                .builder()
                .menuConfigurationId(payload.getMenuConfigurationId())
                .kitchenId(payload.getKitchenId())
                .build();
    }
}
