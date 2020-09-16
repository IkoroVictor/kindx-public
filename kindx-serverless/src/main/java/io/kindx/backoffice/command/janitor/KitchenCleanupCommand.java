package io.kindx.backoffice.command.janitor;

import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.backoffice.service.KitchenService;
import org.apache.commons.lang3.StringUtils;

public class KitchenCleanupCommand  implements JanitorCommand {

    private KitchenService kitchenService;

    public KitchenCleanupCommand(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @Override
    public void execute(JanitorEvent event) {
        switch (event.getType()) {
            case KITCHEN:
                String kitchenId = StringUtils.isNotBlank(event.getValue())
                        ? event.getValue()
                        : event.getKitchenId();
                kitchenService.cleanupKitchenById(kitchenId);
                break;
            default:
                throw new IllegalArgumentException("Unknown kitchen Cleanup event type " + event.getType());
        }
    }
}
