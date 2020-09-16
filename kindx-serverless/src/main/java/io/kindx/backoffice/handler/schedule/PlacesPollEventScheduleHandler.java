package io.kindx.backoffice.handler.schedule;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import io.kindx.backoffice.dto.events.PlacesPollEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.constants.Defaults;
import io.kindx.dao.LocationDao;
import io.kindx.entity.Location;
import io.kindx.factory.InjectorFactory;
import io.kindx.util.LocationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlacesPollEventScheduleHandler extends ScheduleEventHandler {


    private static final Logger logger = LogManager.getLogger(PlacesPollEventScheduleHandler.class);

    private final LocationDao locationDao;
    private final QueueService queueService;

    public PlacesPollEventScheduleHandler() {
        super(InjectorFactory.getInjector().getInstance(EventService.class));
        this.locationDao = InjectorFactory.getInjector().getInstance(LocationDao.class);
        this.queueService = InjectorFactory.getInjector().getInstance(QueueService.class);
    }

    @Override
    protected Object handleScheduledEvent(ScheduledEvent event) {
        List<Location> activeLocations = locationDao.getActiveLocations();
        int eventSum = activeLocations.stream().mapToInt(this::mapAndEnqueueEvents).sum();

        Map<String, Object> results = new HashMap<>();
        results.put("totalPollEvents", eventSum);
        results.put("activeLocationsCount", activeLocations.size());
        return results;
    }

    private int mapAndEnqueueEvents(Location location) {
        List<PlacesPollEvent> events = mapToEvents(location);
        if (!events.isEmpty()) {
            queueService.enqueuePlacesPollEventMessages(events);
        }
        return events.size();
    }

    private List<PlacesPollEvent> mapToEvents(Location location) {
        Integer radius = location.getSearchRadius();
        Integer factor = location.getRadiusReductionFactor();
        if (radius == null || radius > Defaults.MAX_LOCATION_SEARCH_RADIUS_METERS || radius < 0 ) {
            logger.warn("Invalid search radius for {}, '{}'.....using max '{}'", radius,
                    location.getLocationId(), Defaults.MAX_LOCATION_SEARCH_RADIUS_METERS);
            radius = Defaults.MAX_LOCATION_SEARCH_RADIUS_METERS;
        }

        if (factor == null || factor > Defaults.MAX_RADIUS_FACTOR_METERS || factor < 0 ) {
            logger.warn("Invalid radius factor for {}, '{}'.....using max '{}'", factor,
                    location.getLocationId(), Defaults.MAX_RADIUS_FACTOR_METERS);
            factor = Defaults.MAX_RADIUS_FACTOR_METERS;
        }

        List<PlacesPollEvent> events = new ArrayList<>();
        List<Map<String, Double>> points = LocationUtils.getOverlappingPackedCirclePoints(location.getLat(),
                location.getLon(), radius, factor);
        for (int i = 0; i < points.size(); i++) {
            Map<String, Double> m = points.get(i);
            PlacesPollEvent build = PlacesPollEvent.builder()
                    .id(location.getLocationId() + "_" + i)
                    .locationId(location.getLocationId())
                    .name(location.getName())
                    .lat(m.get(LocationUtils.LAT_KEY))
                    .lon(m.get(LocationUtils.LON_KEY))
                    .radiusInMeters(m.get(LocationUtils.MAX_RADIUS_KEY).intValue())
                    .build();
            events.add(build);
        }
        return events;
    }


}
