package io.kindx.backoffice.handler.schedule;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import io.kindx.backoffice.dto.events.MenuCrawlEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.MenuConfigurationDao;
import io.kindx.entity.Kitchen;
import io.kindx.entity.MenuConfiguration;
import io.kindx.exception.NotFoundException;
import io.kindx.factory.InjectorFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class CrawlEventScheduleHandler extends ScheduleEventHandler {

    private static final Logger logger = LogManager.getLogger(CrawlEventScheduleHandler.class);
    private static final String LOG_FORMAT = "[EventId: %s] [Message: %s]";
    private QueueService queueService;
    private MenuConfigurationDao menuConfigurationDao;
    private KitchenDao kitchenDao;

    public CrawlEventScheduleHandler() {
        super(InjectorFactory.getInjector().getInstance(EventService.class));
        this.kitchenDao = InjectorFactory.getInjector().getInstance(KitchenDao.class);
        this.menuConfigurationDao = InjectorFactory.getInjector().getInstance(MenuConfigurationDao.class);
        this.queueService = InjectorFactory.getInjector().getInstance(QueueService.class);
    }

    @Override
    protected Object handleScheduledEvent(ScheduledEvent event) {
        return handleCrawlEvent(event);
    }

    private Object handleCrawlEvent(ScheduledEvent event) {
        Map<MenuConfigurationType, Object> resultMap = new HashMap<>();
        for (MenuConfigurationType t : Arrays.asList(MenuConfigurationType.PAGE, MenuConfigurationType.PDF_URL)) {
            resultMap.put(t, publishCrawlEventsForType(event.getId(), t));
        }
        return resultMap;
    }

    private Object publishCrawlEventsForType(String eventId, MenuConfigurationType type) {
        List<MenuConfiguration> allMenuConfigs = menuConfigurationDao.getMenuConfigurationsByType(type);

        logger.info(String.format(LOG_FORMAT, eventId, "Publishing {} crawl event(s) for {} configurations..."),
                allMenuConfigs.size(),
                type);
        Map<String, List<MenuConfiguration>> grouped = allMenuConfigs.stream()
                .collect(Collectors.groupingBy(MenuConfiguration::getKitchenId));

        int failed = 0;
        int failedKitchens = 0;
        int total = 0;

        for (Map.Entry<String, List<MenuConfiguration>> g : grouped.entrySet()) {
            total += grouped.size();
            try {
                publishCrawlEvents(type, g.getValue());
                kitchenDao.save(() -> updateTimestamp(g.getKey()));
            } catch (Exception ex) {
                logger.error("Error publishing crawl {} events for kitchen: {} - {}",
                        type, g.getKey(), ex.getMessage(), ex);
                failed += grouped.size();
                failedKitchens++;
            }
        }
        logger.info(String.format(LOG_FORMAT, eventId, "{} {} configuration crawl event(s) published..."),
                allMenuConfigs.size(),
                type);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("totalKitchens", grouped.keySet().size());
        result.put("failed", failed);
        result.put("failedKitchens", failedKitchens);
        return result;
    }

    private Kitchen updateTimestamp(String kitchenId) {
        Optional<Kitchen> optional = kitchenDao.getActiveKitchenByKitchenId(kitchenId);
        if (optional.isPresent()) {
            Kitchen kitchen = optional.get();
            kitchen.setLastJobTimestamp(new Date().getTime());
            return kitchen;
        }
        throw new NotFoundException(kitchenId + " - Kitchen not found");
    }


    private void publishCrawlEvents(MenuConfigurationType type,
                                    List<MenuConfiguration> configurations) {
        if (type == MenuConfigurationType.PAGE) {
            queueService.enqueueMenuCrawlEventMessages(
                    configurations.stream().map(m -> MenuCrawlEvent.builder()
                            .contentType(MenuCrawlEvent.ContentType.HTML)
                            .kitchenId(m.getKitchenId())
                            .menuConfigurationId(m.getId())
                            .url(m.getValue())
                            .build())
                            .collect(Collectors.toList())
            );
        }

        if (type == MenuConfigurationType.PDF_URL) {
            queueService.enqueueMenuCrawlEventMessages(
                    configurations.stream().map(m -> MenuCrawlEvent.builder()
                            .contentType(MenuCrawlEvent.ContentType.PDF)
                            .kitchenId(m.getKitchenId())
                            .menuConfigurationId(m.getId())
                            .url(m.getValue())
                            .build())
                            .collect(Collectors.toList())
            );
        }

    }
}
