package io.kindx.gateway.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.kindx.backoffice.dto.events.PreferenceReprocessEvent;
import io.kindx.backoffice.dto.events.PreferencesEvent;
import io.kindx.backoffice.dto.user.UserLastLocation;
import io.kindx.backoffice.processor.notification.NotificationChannel;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.constants.Defaults;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.MenuFoodItemDao;
import io.kindx.dao.UserDao;
import io.kindx.dao.UserKitchenMappingDao;
import io.kindx.dto.GeoPointDto;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.User;
import io.kindx.entity.UserKitchenMapping;
import io.kindx.exception.NotFoundException;
import io.kindx.gateway.dto.UserDto;
import io.kindx.gateway.dto.UserKitchenMappingCreateDto;
import io.kindx.gateway.dto.UserKitchenMappingDto;
import io.kindx.gateway.dto.UserUpdateDto;
import io.kindx.gateway.exception.InvalidRequestException;
import io.kindx.mapper.UserMapper;
import io.kindx.util.IDUtil;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;

import java.util.*;

import static io.kindx.backoffice.dto.events.PreferenceReprocessEvent.ReprocessType.USER_LOCATION_UPDATE;

public class UserFacade {

    private static final Logger logger = LogManager.getLogger(UserFacade.class);


    private UserMapper mapper;
    private UserDao userDao;
    private UserKitchenMappingDao userKitchenMappingDao;
    private MenuFoodItemDao menuFoodItemDao;
    private KitchenDao kitchenDao;
    private EventService eventService;
    private QueueService queueService;
    private ElasticSearchService elasticSearchService;
    private ObjectMapper objectMapper;
    private long reprocessRadiusInMeters;

    @Inject
    public UserFacade(UserMapper mapper,
                      UserDao userDao,
                      UserKitchenMappingDao userKitchenMappingDao,
                      MenuFoodItemDao menuFoodItemDao,
                      KitchenDao kitchenDao,
                      EventService eventService,
                      QueueService queueService,
                      ElasticSearchService elasticSearchService,
                      ObjectMapper objectMapper,
                      @Named("reprocessRadiusInMeters") long reprocessRadiusInMeters) {

        this.mapper = mapper;
        this.userDao = userDao;
        this.userKitchenMappingDao = userKitchenMappingDao;
        this.menuFoodItemDao = menuFoodItemDao;
        this.kitchenDao = kitchenDao;
        this.eventService = eventService;
        this.queueService = queueService;
        this.elasticSearchService = elasticSearchService;
        this.objectMapper = objectMapper;
        this.reprocessRadiusInMeters = reprocessRadiusInMeters;
    }

    public void addUserKitchenMapping(String userId, String kitchenId,
                                      UserKitchenMappingCreateDto dto) {
        this.saveUserKitchenMapping(userId, kitchenId, dto.getPreferences(), Boolean.TRUE.equals(dto.getShouldNotify()));
    }

    public UserKitchenMappingDto getUserKitchenMapping(String userId, String kitchenId) {
        UserKitchenMapping mapping = userKitchenMappingDao.getUserKitchenMapping(userId, kitchenId)
                .orElseThrow(() -> new NotFoundException("User kitchen mapping not found."));
        return mapper.toUserKitchenMappingDto(mapping);
    }

    public void deleteUserKitchenMapping(String userId, String kitchenId) {
        menuFoodItemDao.getUserFoodItemsByKitchen(userId, kitchenId)
                .forEach(menuFoodItemDao::delete);
        userKitchenMappingDao.delete(UserKitchenMapping.builder().userId(userId).kitchenId(kitchenId).build());
    }

    public void addUserLastLocation(String userId, GeoPointDto geoPointDto) {
        this.updateUserLastLocation(userId, geoPointDto);
    }

    public UserDto getUser(String userId) {
        User user = getOrCreateUser(userId);
        return mapper.toUserDto(user);
    }

    public UserDto updateUser(String userId, UserUpdateDto updateDto) {
        validateUserUpdate(updateDto);
        User user = getOrCreateUser(userId);
        user.setGeneralFoodPreferences(updateDto.getGeneralFoodPreferences());
        user.setLocale(updateDto.getLocale());
        userDao.forceSave(user);
        triggerUserReprocess(userId, USER_LOCATION_UPDATE, null);
        return mapper.toUserDto(user);
    }

    private User getOrCreateUser(String userId) {
        Optional<User> userOptional = userDao.getUser(userId);
        if (userOptional.isPresent()) {
            return userOptional.get();
        }
        User newUser = User.builder()
                .userId(userId)
                .isDisabled(false)
                .locale(Defaults.USER_LOCALE)
                .createdTimestamp(new Date().getTime())
                .notificationChannel(NotificationChannel.NONE)
                .build();

        userDao.forceSave(() -> newUser);
        return newUser;
    }

    private void saveUserKitchenMapping(String userId,
                                        String kitchenId,
                                        Set<String> preferences,
                                        boolean shouldNotify) {

        kitchenDao.getKitchenByKitchenId(kitchenId).orElseThrow(() -> new NotFoundException("Kitchen does not exist"));
        getOrCreateUser(userId);
        userKitchenMappingDao.save(() -> buildUserKitchenMapping(userId, kitchenId, preferences, shouldNotify));

        Set<String> newPreferences = preferences == null ? Collections.emptySet() : preferences;

        //Delete previous menu food items not in current preferences
        //TODO: Currently case-sensitive. Find a case insensitive approach
        menuFoodItemDao.getUserFoodItemsByKitchen(userId, kitchenId)
                .stream()
                .filter(m -> !newPreferences.contains(m.getName()))
                .forEach(menuFoodItemDao::delete);

        if (!newPreferences.isEmpty()) {
            eventService.publishPreferencesEvent(PreferencesEvent
                    .builder()
                    .id(IDUtil.generatePreferencesId())
                    .type(PreferencesEvent.Type.KITCHEN)
                    .preferences(newPreferences)
                    .kitchenId(kitchenId)
                    .userId(userId)
                    .build());
        }
    }

    private UserKitchenMapping buildUserKitchenMapping(String userId,
                                                       String kitchenId,
                                                       Set<String> preferences,
                                                       boolean shouldNotify) {
        UserKitchenMapping mapping = userKitchenMappingDao.getUserKitchenMapping(userId, kitchenId)
                .orElse(UserKitchenMapping.builder()
                        .userId(userId)
                        .kitchenId(kitchenId)
                        .isDisabled(false)
                        .createdTimestamp(new Date().getTime())
                        .build());
        mapping.setShouldNotify(shouldNotify);
        if (preferences != null && preferences.isEmpty()) {
            preferences = null; //Dynamodb compatibility
        }
        mapping.setFoodPreferences(preferences);
        return mapping;
    }

    private void updateUserLastLocation(String userId, GeoPointDto point) {
        UserLastLocation locationUpdate = UserLastLocation
                .builder()
                .createdTimestamp(new Date().getTime())
                .userId(userId)
                .geoPoint(point)
                .build();
        elasticSearchService.putInUserLastLocation(locationUpdate, userId);
        triggerUserReprocess(userId, USER_LOCATION_UPDATE, point);

    }

    private void validateUserUpdate(UserUpdateDto updateDto) {
        if (Locale.forLanguageTag(updateDto.getLocale()) == null) {
            throw new InvalidRequestException("Unknown locale " + updateDto.getLocale());
        }
    }

    @SneakyThrows
    private void triggerUserReprocess(String userId,
                                     PreferenceReprocessEvent.ReprocessType type,
                                     GeoPointDto pointOfFocus) {
        if (pointOfFocus == null) {
            GetResponse locationResponse = elasticSearchService.getUserLastLocation(userId);
            if (!locationResponse.isExists()) {
                logger.info("No last location found for user '{}' ...reprocess skipped", userId);
                return;
            }
            pointOfFocus = objectMapper.readValue(locationResponse.getSourceAsBytes(), UserLastLocation.class).getGeoPoint();
        }
        queueService.enqueuePreferenceReprocessMessages(Collections.singletonList(PreferenceReprocessEvent
                .builder()
                .id(IDUtil.generateReprocessId())
                .type(type)
                .pointOfFocus(pointOfFocus)
                .userId(userId)
                .searchRadiusInMeters(reprocessRadiusInMeters)
                .build()));
    }

}
