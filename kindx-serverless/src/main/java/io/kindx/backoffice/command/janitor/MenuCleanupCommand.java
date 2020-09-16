package io.kindx.backoffice.command.janitor;

import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.backoffice.service.MenuService;

public class MenuCleanupCommand implements JanitorCommand {

    private MenuService menuService;

    public MenuCleanupCommand(MenuService menuService) {
        this.menuService = menuService;
    }

    @Override
    public void execute(JanitorEvent event) {
        switch (event.getType()) {
            case MENUS_BY_MENU_CONFIG:
                menuService.deleteMenusViaConfigId(event.getValue());
                break;
            case MENU:
                menuService.deleteMenuViaMenuId(event.getKitchenId(), event.getValue());
                break;
            case MENUS_BY_KITCHEN:
                menuService.deleteMenusViaKitchenId(event.getValue());
                break;
            default: throw new IllegalArgumentException("Unknown Menu Cleanup event type " + event.getType());
        }
    }
}
