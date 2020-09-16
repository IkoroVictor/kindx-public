package io.kindx.backoffice.command.janitor;

import io.kindx.backoffice.dto.events.JanitorEvent;

public interface JanitorCommand {
    void execute(JanitorEvent event);
}
