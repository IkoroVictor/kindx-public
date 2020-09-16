package io.kindx.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.maps.GeoApiContext;
import com.google.maps.PlaceDetailsRequest;
import com.google.maps.PlacesApi;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import io.kindx.dao.PlacesCacheDao;
import io.kindx.entity.PlacesCacheEntry;
import lombok.SneakyThrows;

import java.util.Date;
import java.util.Optional;


public class PlacesApiClient {


    private long placesCacheTtl;
    private GeoApiContext geoApiContext;
    private ObjectMapper objectMapper;
    private PlacesCacheDao placesCacheDao;

    @Inject
    public PlacesApiClient(@Named("placesCacheTtlSeconds") long placesCacheTtl,
                           GeoApiContext geoApiContext,
                           ObjectMapper objectMapper,
                           PlacesCacheDao placesCacheDao) {
        this.placesCacheTtl = placesCacheTtl;
        this.geoApiContext = geoApiContext;
        this.objectMapper = objectMapper;
        this.placesCacheDao = placesCacheDao;
    }

    @SneakyThrows
    public PlacesSearchResponse searchNearby(double pointOfFocusLat, double pointOfFocusLon,
                                             int radiusInMeters, PlaceType placeType ) {
         return PlacesApi.nearbySearchQuery(geoApiContext,
                new LatLng(pointOfFocusLat, pointOfFocusLon))
                .radius(radiusInMeters)
                .type(placeType)
                .await();
    }

    @SneakyThrows
    public PlacesSearchResponse searchNearbyNextPage(String pageToken ) {
        return PlacesApi.nearbySearchNextPage(geoApiContext, pageToken).await();
    }

    //TODO: ElasticCaching needed
    @SneakyThrows
    public PlaceDetails getPlaceDetails(String placeId) {
        long currentTime = new Date().getTime();
        Optional<PlacesCacheEntry> entry = placesCacheDao.getEntry(placeId);
        if (entry.isPresent() && entry.get().getCreatedTimestamp() > (currentTime - (placesCacheTtl * 1000))) {
            return objectMapper.readValue(entry.get().getDetails(), PlaceDetails.class);
        }
        PlaceDetails details = getPlaceFromPlacesApi(placeId);
        placesCacheDao.forceSave(PlacesCacheEntry.builder()
                .placesId(details.placeId)
                .details(objectMapper.writeValueAsString(details))
                .createdTimestamp(currentTime)
                .build());
        return details;
    }


    @SneakyThrows
    private PlaceDetails getPlaceFromPlacesApi(String placeId) {
        return PlacesApi.placeDetails(geoApiContext, placeId)
                .fields(PlaceDetailsRequest.FieldMask.PLACE_ID,
                        PlaceDetailsRequest.FieldMask.NAME,
                        PlaceDetailsRequest.FieldMask.GEOMETRY_LOCATION,
                        PlaceDetailsRequest.FieldMask.INTERNATIONAL_PHONE_NUMBER,
                        PlaceDetailsRequest.FieldMask.OPENING_HOURS,
                        PlaceDetailsRequest.FieldMask.WEBSITE,
                        PlaceDetailsRequest.FieldMask.PERMANENTLY_CLOSED,
                        PlaceDetailsRequest.FieldMask.ADDRESS_COMPONENT,
                        PlaceDetailsRequest.FieldMask.URL,
                        PlaceDetailsRequest.FieldMask.FORMATTED_ADDRESS)
                .await();
    }

    @SneakyThrows
    private PlaceDetails getPlaceDetails(String placeId, PlaceDetailsRequest.FieldMask... fields) {
        return PlacesApi.placeDetails(geoApiContext, placeId)
                .fields(fields)
                .await();
    }

}
