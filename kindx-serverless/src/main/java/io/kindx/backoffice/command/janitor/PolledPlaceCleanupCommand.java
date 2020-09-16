package io.kindx.backoffice.command.janitor;

import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.elasticsearch.ElasticSearchService;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.rest.RestStatus;

public class PolledPlaceCleanupCommand implements JanitorCommand {
    private ElasticSearchService service;

    public PolledPlaceCleanupCommand(ElasticSearchService service) {
        this.service = service;
    }
    @Override
    public void execute(JanitorEvent event) {
        switch (event.getType()) {
            case POLLED_PLACES:
                deletePlace(event.getValue());
                break;
            default: throw new IllegalArgumentException("Unknown Polled Place Cleanup event type " + event.getType());
        }
    }

    private void deletePlace(String placeId) {
        DeleteResponse deleteResponse = service.deletePolledPlace(placeId);
        if (deleteResponse.status() != RestStatus.OK) {
            throw new RuntimeException(String.format("Could not delete polled place %s. Status code %d",
                    placeId, deleteResponse.status().getStatus()));
        }
    }
}
