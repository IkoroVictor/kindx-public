package io.kindx.backoffice.service;

import io.kindx.backoffice.command.Commands;
import io.kindx.backoffice.dto.events.JanitorEvent;

public class JanitorService {

    public void processEvent(JanitorEvent event) {
        Commands.of(event.getType()).execute(event);
    }

}
