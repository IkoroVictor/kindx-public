package io.kindx.backoffice.command;

import io.kindx.backoffice.command.janitor.JanitorCommand;
import io.kindx.backoffice.command.janitor.KitchenCleanupCommand;
import io.kindx.backoffice.command.janitor.MenuCleanupCommand;
import io.kindx.backoffice.command.janitor.PolledPlaceCleanupCommand;
import io.kindx.backoffice.service.KitchenService;
import io.kindx.backoffice.service.MenuService;
import io.kindx.constants.JanitorEventType;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.factory.InjectorFactory;

public class Commands {

    public static JanitorCommand of(JanitorEventType type) {
        switch (type) {
            case MENU:
            case MENUS_BY_MENU_CONFIG:
            case MENUS_BY_KITCHEN:
                return new MenuCleanupCommand(InjectorFactory.getInjector().getInstance(MenuService.class));
            case POLLED_PLACES:
                return new PolledPlaceCleanupCommand(InjectorFactory.getInjector().getInstance(ElasticSearchService.class));
            case KITCHEN:
                return new KitchenCleanupCommand(InjectorFactory.getInjector().getInstance(KitchenService.class));
            default: throw new IllegalArgumentException("Unknown Janitor event type - " + type);
        }
    }
}
