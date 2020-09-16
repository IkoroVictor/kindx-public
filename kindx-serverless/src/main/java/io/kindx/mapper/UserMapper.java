package io.kindx.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.kindx.backoffice.dto.user.UserLastLocation;
import io.kindx.dao.LocationDao;
import io.kindx.dto.GeoPointDto;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.Location;
import io.kindx.entity.User;
import io.kindx.entity.UserKitchenMapping;
import io.kindx.gateway.dto.LocationDto;
import io.kindx.gateway.dto.UserDto;
import io.kindx.gateway.dto.UserKitchenMappingDto;
import lombok.SneakyThrows;
import org.elasticsearch.action.get.GetResponse;

import java.util.stream.Collectors;

public class UserMapper {

    private LocationDao locationDao;
    private ElasticSearchService elasticSearchService;
    private ObjectMapper objectMapper;

    @Inject
    public UserMapper(LocationDao locationDao,
                      ElasticSearchService elasticSearchService,
                      ObjectMapper objectMapper) {
        this.locationDao = locationDao;
        this.elasticSearchService = elasticSearchService;
        this.objectMapper = objectMapper;
    }

    public UserKitchenMappingDto toUserKitchenMappingDto(UserKitchenMapping mapping) {
        return UserKitchenMappingDto.builder()
                .preferences(mapping.getFoodPreferences())
                .kitchenId(mapping.getKitchenId())
                .userId(mapping.getUserId())
                .shouldNotify(mapping.getShouldNotify())
                .build();

    }

    public UserDto toUserDto(User user) {
        return UserDto.builder()
                .generalFoodPreferences(user.getGeneralFoodPreferences())
                .notificationChannel(user.getNotificationChannel())
                .locale(user.getLocale())
                .userId(user.getUserId())
                .locations(locationDao.getActiveLocations().stream()
                        .map(this::mapToLocationDto)
                        .collect(Collectors.toList()))
                .userLastLocation(getUserLastLocation(user.getUserId()))
                .build();
    }

    private LocationDto mapToLocationDto(Location location) {
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .id(location.getLocationId())
                .name(location.getName())
                .build();
    }

    @SneakyThrows
    private GeoPointDto getUserLastLocation(String userId) {
        GetResponse response = elasticSearchService.getUserLastLocation(userId);
        if (!response.isExists()){
            return null;
        }
        return objectMapper.readValue(response.getSourceAsBytes(), UserLastLocation.class).getGeoPoint();
    }
}
