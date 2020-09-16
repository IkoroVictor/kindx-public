package io.kindx.backoffice.service;

import com.google.inject.Inject;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import io.kindx.backoffice.dto.events.PlacesPollEvent;
import io.kindx.backoffice.dto.places.PolledPlacesRestaurant;
import io.kindx.client.PlacesApiClient;
import io.kindx.constants.Defaults;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.LocationDao;
import io.kindx.dto.GeoPointDto;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.Location;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class PlacesPollService {

    private static final Logger logger = LogManager.getLogger(PlacesPollService.class);

    private LocationDao locationDao;
    private PlacesApiClient placesApiClient;
    private KitchenDao kitchenDao;
    private ElasticSearchService elasticSearchService;


    @Inject
    public PlacesPollService(LocationDao locationDao,
                             PlacesApiClient placesApiClient,
                             KitchenDao kitchenDao,
                             ElasticSearchService elasticSearchService) {
        this.locationDao = locationDao;
        this.placesApiClient = placesApiClient;
        this.kitchenDao = kitchenDao;
        this.elasticSearchService = elasticSearchService;
    }

    public Map processPollEvent(PlacesPollEvent event) {
        Map<String, Object> resultMap = new HashMap<>();
        Optional<Location> optional = locationDao.getLocation(event.getLocationId());
        if (!optional.isPresent()) {
            logger.warn("Location with ID {} does not exist. Skipping.......", event.getLocationId());
            return resultMap;
        }
        resultMap.put(PlaceType.RESTAURANT.toString().toLowerCase(),
                processLocation(event, optional.get(), PlaceType.RESTAURANT));
        resultMap.put(PlaceType.CAFE.toString().toLowerCase(),
                processLocation(event, optional.get(), PlaceType.CAFE));
        return resultMap;
    }

    @SneakyThrows
    private Map processLocation(PlacesPollEvent event, Location location, PlaceType placeType) {
        Integer radiusMeters = event.getRadiusInMeters();
        if(radiusMeters == null || radiusMeters > 50000 || radiusMeters < 1) {
            logger.warn("Invalid PlacesPollEvent radius (meters) '{}' for location: '{}'.....defaulting to '{}'",
                    radiusMeters, location.getLocationId(),
                    Defaults.DEFAULT_PLACES_POLL_SEARCH_RADIUS_METERS);
            radiusMeters =  Defaults.DEFAULT_PLACES_POLL_SEARCH_RADIUS_METERS;
        }

        int totalCount = 0;
        int totalSaved = 0;
        PlacesSearchResponse response = placesApiClient.searchNearby(event.getLat(),
                event.getLon(), radiusMeters, placeType);

        PlacesSearchResult[] results = response.results;
        totalCount += results.length;
        totalSaved += putToESIndex(results, location, placeType);
        while (response.nextPageToken != null) {
            response = placesApiClient.searchNearbyNextPage(response.nextPageToken);
            results = response.results;
            totalCount += results.length;
            totalSaved += putToESIndex(results, location, placeType);
        }
        Map<String, Object> totalMap = new HashMap<>();
        totalMap.put("totalCount", totalCount);
        totalMap.put("totalSaved", totalSaved);
        totalMap.put("radiusInMeters", radiusMeters);
        return totalMap;
    }

    private int putToESIndex(PlacesSearchResult[] results, Location location, PlaceType type) {
        Map<String, PolledPlacesRestaurant> polledPlacesMap = new HashMap<>();
        for (PlacesSearchResult result: results) {
            if(kitchenDao.getKitchensByPlacesId(result.placeId).isEmpty()
                    && !result.permanentlyClosed) {
                try {
                    PolledPlacesRestaurant polledPlace = mapToPolledRestaurant(result, location, type);
                    polledPlacesMap.put(polledPlace.getPlacesId(), polledPlace);
                } catch (Exception ex) {
                    logger.error("Could not map place with place id {} for location {}.....skipping",
                            result.placeId, location.getLocationId(), ex);
                }
            }
        }
        if (!polledPlacesMap.isEmpty()) {
            elasticSearchService.putBulkInPolledPlacesIndex(polledPlacesMap, false);
        }
        return polledPlacesMap.size();
    }

    @SneakyThrows
    private PolledPlacesRestaurant mapToPolledRestaurant(PlacesSearchResult result, Location location, PlaceType type) {
        PlaceDetails details = placesApiClient.getPlaceDetails(result.placeId);

        PolledPlacesRestaurant.PolledPlacesRestaurantBuilder builder = PolledPlacesRestaurant.builder()
                .placesId(result.placeId)
                .address(details.formattedAddress)
                .name(result.name)
                .geoPoint(GeoPointDto.builder()
                        .lat(result.geometry.location.lat)
                        .lon(result.geometry.location.lng).build())
                .locationId(location.getLocationId())
                .types(Arrays.asList(result.types))
                .menuPageUrls(Collections.emptyList())
                .pdfUrls(Collections.emptyList())
                .phone(details.formattedPhoneNumber)
                .internationalPhone(details.internationalPhoneNumber)
                .rating(result.rating)
                .totalRatings(result.userRatingsTotal)
                .createdTimestamp(new Date().getTime())
                .defaultLanguages(location.getDefaultLanguages())
                .placeType(type.toString())
                .validated(false);
        if (details.openingHours != null) {
            builder.openingHours(details.openingHours.toString());
        }
        if (result.icon != null) {
            builder.thumbnailUrl(result.icon.toString());
        }
        if (details.website != null) {
            builder.website(details.website.toString());
        }
        if (details.url != null) {
            builder.placeUrl(details.url.toString());
        }

        return builder.build();
    }
}
