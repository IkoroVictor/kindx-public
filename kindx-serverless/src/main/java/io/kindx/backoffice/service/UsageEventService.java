package io.kindx.backoffice.service;

import com.google.inject.Inject;
import io.kindx.backoffice.dto.events.UsageEvent;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.util.ResilienceUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UsageEventService {

    private ElasticSearchService elasticSearchService;

    @Inject
    public UsageEventService(ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }


    public void processUsageEvent(UsageEvent event) {
        ResilienceUtil.retryOnExceptionSilently(() -> elasticSearchService.putInUsageEventsIndex(event, event.getEventId()));
    }

    public void processUsageEvents(Collection<UsageEvent> events) {
        Map<String, UsageEvent> eventMap = new HashMap<>();
        events.forEach(e -> eventMap.put(e.getEventId(), e));
        ResilienceUtil.retryOnExceptionSilently(() -> elasticSearchService.putBulkInUsageEventsIndex(eventMap));
    }


}
