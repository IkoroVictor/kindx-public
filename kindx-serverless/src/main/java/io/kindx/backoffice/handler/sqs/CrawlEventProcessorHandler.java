package io.kindx.backoffice.handler.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.backoffice.dto.events.MenuCrawlEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.MenuCrawlerService;
import io.kindx.factory.InjectorFactory;

public class CrawlEventProcessorHandler extends SqsEventHandler<MenuCrawlEvent> {

    private final MenuCrawlerService crawlerService;

    public CrawlEventProcessorHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class),
                MenuCrawlEvent.class);
        crawlerService = InjectorFactory.getInjector().getInstance(MenuCrawlerService.class);
    }

    @Override
    protected Object processEventPayload(MenuCrawlEvent payload) {
        crawlerService.processMenuCrawlEvent(payload);
        return null;
    }

    @Override
    protected Object usagePayload(MenuCrawlEvent payload) {
        return payload;
    }

}
