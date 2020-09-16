package io.kindx.backoffice.service;

import com.google.inject.Inject;
import io.kindx.backoffice.dto.events.PreferenceReprocessEvent;
import io.kindx.backoffice.dto.events.PreferencesEvent;
import io.kindx.dao.UserDao;
import io.kindx.dto.GeoPointDto;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.User;
import io.kindx.util.IDUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.*;
import java.util.stream.Stream;

public class PreferenceReprocessService  {

    private final String ES_MENU_KITCHEN_ID_FIELD = "kitchenId";
    private final String ES_MENU_GEO_POINT_FIELD = "location.geoPoint";
    private final String ES_USER_LAST_LOCATION_GEO_POINT_FIELD = "geoPoint";

    private static final Logger logger = LogManager.getLogger(PreferenceReprocessService.class);

    private UserDao userDao;
    private EventService eventService;
    private ElasticSearchService elasticSearchService;

    @Inject
    public PreferenceReprocessService(UserDao userDao,
                                      EventService eventService,
                                      ElasticSearchService elasticSearchService) {
        this.userDao = userDao;
        this.eventService = eventService;
        this.elasticSearchService = elasticSearchService;
    }


    public Map handleReprocessEvent(PreferenceReprocessEvent event) {
        switch (event.getType()) {
            case USER_LOCATION_UPDATE:
            case USER_PREFERENCE_UPDATE:
                return processUserUpdate(event);
            case KITCHEN_MENU_UPDATE:
                return processKitchenMenuUpdate(event);
            default:
                return Collections.emptyMap();
        }
    }

    private Map processUserUpdate(PreferenceReprocessEvent event) {
        validateEvent(event);
        Set<String> userPrefs  = getUserPreferences(event.getUserId());
        if (userPrefs.isEmpty()) {
            logger.warn("No general food preferences for user {}.....skipping", event.getUserId());
            return Collections.singletonMap("processed", 0);
        }
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders
                .boolQuery()
                .must(QueryBuilders.matchAllQuery())
                .filter(esGeoFilter(ES_MENU_GEO_POINT_FIELD,
                        event.getPointOfFocus(), event.getSearchRadiusInMeters())))
                .fetchSource(ES_MENU_KITCHEN_ID_FIELD,null);

        SearchResponse response = elasticSearchService.searchMenuIndex(sourceBuilder, true);
        long processed = 0;

        do {
            Map<String, String> menuKitchenMap = new HashMap<>();
            List<PreferencesEvent> events =  new ArrayList<>();
            Stream.of(response.getHits().getHits()).forEach(h -> {
                menuKitchenMap.put(h.getId(), h.field(ES_MENU_KITCHEN_ID_FIELD).getValue());
            });
            menuKitchenMap.forEach((m, k) -> {
                events.add(PreferencesEvent.builder()
                        .type(PreferencesEvent.Type.MENU)
                        .kitchenId(k)
                        .menuId(m)
                        .userId(event.getUserId())
                        .preferences(userPrefs)
                        .id(IDUtil.generatePreferencesId())
                        .build());
            });
            if (events.size() > 0) {
                eventService.publishPreferencesEvents(events);
                processed += events.size();
            }
            String prevScrollId  = response.getScrollId();
            response = elasticSearchService.searchScroll(prevScrollId, 60);

            elasticSearchService.clearScroll(prevScrollId);
        }
        while (response.getHits() != null
                && response.getHits().getHits() != null
                && response.getHits().getHits().length > 0);
        return Collections.singletonMap("processed", processed);
    }

    private Map processKitchenMenuUpdate(PreferenceReprocessEvent event) {
        validateEvent(event);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders
                .boolQuery()
                .must(QueryBuilders.matchAllQuery())
                .filter(esGeoFilter(ES_USER_LAST_LOCATION_GEO_POINT_FIELD,
                        event.getPointOfFocus(),
                        event.getSearchRadiusInMeters())))
                .fetchSource(false);

        SearchResponse response = elasticSearchService.searchUserLastLocationIndex(sourceBuilder);
        long processed = 0;

        do {
            List<PreferencesEvent> events =  new ArrayList<>();
            Stream.of(response.getHits().getHits()).forEach(h -> {
                String userId = h.getId();
                events.add(PreferencesEvent.builder()
                        .type(PreferencesEvent.Type.MENU)
                        .kitchenId(event.getKitchenId())
                        .menuId(event.getMenuId())
                        .userId(userId)
                        .preferences(getUserPreferences(userId))
                        .id(IDUtil.generatePreferencesId())
                        .build());
            });
            if (events.size() > 0) {
                eventService.publishPreferencesEvents(events);
                processed += events.size();
            }
            response = elasticSearchService.searchScroll(response.getScrollId(), 60);
        }
        while (response.getHits() != null
                && response.getHits().getHits() != null
                && response.getHits().getHits().length > 0);
        return Collections.singletonMap("processed", processed);

    }



    private Set<String> getUserPreferences(String userId) {
        Optional<User> optional = userDao.getUser(userId);
        if (!optional.isPresent() || optional.get().getGeneralFoodPreferences() == null) {
            return Collections.emptySet();
        }
        return optional.get().getGeneralFoodPreferences();
    }

    private void validateEvent(PreferenceReprocessEvent event) {
        if (event.getType() == PreferenceReprocessEvent.ReprocessType.KITCHEN_MENU_UPDATE) {
            Objects.requireNonNull(event.getPointOfFocus());
            Objects.requireNonNull(event.getMenuId());
            Objects.requireNonNull(event.getKitchenId());
            Objects.requireNonNull(event.getSearchRadiusInMeters());
        }

        if(event.getType() == PreferenceReprocessEvent.ReprocessType.USER_LOCATION_UPDATE
                || event.getType() == PreferenceReprocessEvent.ReprocessType.USER_PREFERENCE_UPDATE) {
            Objects.requireNonNull(event.getPointOfFocus());
            Objects.requireNonNull(event.getSearchRadiusInMeters());
            Objects.requireNonNull(event.getUserId());
        }

    }

    private GeoDistanceQueryBuilder esGeoFilter(String geoField, GeoPointDto point, long distanceInMeters) {
        return QueryBuilders.geoDistanceQuery(geoField)
                .point(point.getLat(), point.getLon())
                .ignoreUnmapped(true)
                .distance(distanceInMeters, DistanceUnit.METERS);
    }

}
