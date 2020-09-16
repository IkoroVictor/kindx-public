package io.kindx.gateway.facade.admin;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.maps.model.PlaceDetails;
import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.client.FacebookClient;
import io.kindx.client.PlacesApiClient;
import io.kindx.constants.Defaults;
import io.kindx.constants.JanitorEventType;
import io.kindx.constants.LocationSource;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.dao.KitchenConfigurationDao;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.MenuConfigurationDao;
import io.kindx.dto.facebook.FacebookPageDto;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.Kitchen;
import io.kindx.entity.KitchenConfiguration;
import io.kindx.entity.MenuConfiguration;
import io.kindx.exception.NotFoundException;
import io.kindx.gateway.dto.KitchenCreateDto;
import io.kindx.gateway.dto.KitchenDto;
import io.kindx.gateway.dto.KitchenUpdateDto;
import io.kindx.gateway.dto.MenuConfigurationDto;
import io.kindx.gateway.exception.ConflictException;
import io.kindx.gateway.exception.InvalidRequestException;
import io.kindx.mapper.KitchenMapper;
import io.kindx.util.IDUtil;
import io.kindx.util.ResilienceUtil;
import io.kindx.util.TextUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminKitchenFacade {

    private static final Logger logger = LogManager.getLogger(AdminKitchenFacade.class);


    private final String facebookAccessToken;

    private static final String FACEBOOK_HOST = "https://facebook.com/";
    private final KitchenMapper kitchenMapper;
    private PlacesApiClient placesApiClient;
    private FacebookClient facebookClient;
    private KitchenDao kitchenDao;
    private KitchenConfigurationDao kitchenConfigurationDao;
    private MenuConfigurationDao menuConfigurationDao;
    private QueueService queueService;
    private EventService eventService;
    private ElasticSearchService elasticSearchService;

    @Inject
    public AdminKitchenFacade(@Named("facebookToken") String facebookToken,
                              PlacesApiClient placesApiClient,
                              FacebookClient facebookClient,
                              KitchenDao kitchenDao,
                              KitchenConfigurationDao kitchenConfigurationDao,
                              MenuConfigurationDao menuConfigurationDao,
                              QueueService queueService,
                              EventService eventService,
                              ElasticSearchService elasticSearchService) {
        this.placesApiClient = placesApiClient;
        this.facebookClient = facebookClient;
        this.kitchenDao = kitchenDao;
        this.kitchenConfigurationDao = kitchenConfigurationDao;
        this.menuConfigurationDao = menuConfigurationDao;
        this.queueService = queueService;
        this.eventService = eventService;
        this.elasticSearchService = elasticSearchService;
        this.kitchenMapper = new KitchenMapper();
        this.facebookAccessToken  = facebookToken;
    }



    //To support creation via polled restaurants
    public KitchenDto createKitchenFromPlaceDetails(KitchenCreateDto dto,
                                                    PlaceDetails details,
                                                    String creator) {
        List<Kitchen> kitchens = kitchenDao.getKitchensByPlacesId(details.placeId);
        if (kitchens.size() != 0) {
            Kitchen kitchen = kitchens.get(0);
            logger.info("Kitchen with place id '{}' already exists with id '{}'.....skipping",
                    details.placeId, kitchens.get(0).getKitchenId());
            return kitchenMapper.toKitchenDto(
                    kitchen,
                    kitchenConfigurationDao.getKitchenConfiguration(kitchen.getKitchenId(), false)
                            .orElse(KitchenConfiguration.builder().kitchenId(kitchen.getKitchenId()).build()),
                    menuConfigurationDao.getMenuConfigurationsForKitchen(kitchen.getKitchenId()));
        }

        Kitchen.KitchenBuilder kitchenBuilder = Kitchen.builder();
        KitchenConfiguration.KitchenConfigurationBuilder configBuilder = KitchenConfiguration.builder();

        LocationSource locationSource = LocationSource.GOOGLE_PLACES;

        kitchenBuilder.googlePlacesId(details.placeId);
        kitchenBuilder.defaultDisplayName(details.name);
        kitchenBuilder.defaultDisplayAddress(details.formattedAddress);
        if (details.website != null) {
                kitchenBuilder.website(details.website.toString());
        }
        return createKitchenInternal(dto, kitchenBuilder, configBuilder,
                locationSource, null, creator);

    }
    //TODO Menu Examples to sagemaker
    @SneakyThrows
    public KitchenDto createKitchen(KitchenCreateDto dto, String creator) {
        Kitchen.KitchenBuilder kitchenBuilder = Kitchen.builder();
        KitchenConfiguration.KitchenConfigurationBuilder configBuilder = KitchenConfiguration.builder();

        LocationSource locationSource = validateAndResolveLocationSource(
                dto.getPrimaryLocationSource(),
                dto.getPlacesId(),
                dto.getFbPageId());

        String resolvedFacebookId = null;
        if (StringUtils.isNotBlank(dto.getFbPageId())) {
            FacebookPageDto page = resolveFacebookPage(dto.getFbPageId());
            resolvedFacebookId = page.getId();
            validateFacebookId(resolvedFacebookId);
            kitchenBuilder.facebookId(resolvedFacebookId);
            kitchenBuilder.pageUrl(FACEBOOK_HOST + resolvedFacebookId);
            if (locationSource == LocationSource.FACEBOOK_PAGE) {
                kitchenBuilder.defaultDisplayName(page.getName());
                kitchenBuilder.defaultDisplayAddress(page.getSingleLineAddress());
                kitchenBuilder.website(page.getWebsite());
            }
        }

        if (StringUtils.isNotBlank(dto.getPlacesId())) {
            PlaceDetails details = resolveGooglePlaces(dto.getPlacesId());
            String resolvedPlacesId = details.placeId;
            validateGooglePlaces(resolvedPlacesId);
            kitchenBuilder.googlePlacesId(dto.getPlacesId());
            if (locationSource == LocationSource.GOOGLE_PLACES) {
                kitchenBuilder.defaultDisplayName(details.name);
                kitchenBuilder.defaultDisplayAddress(details.formattedAddress);
                if (details.website != null) {
                    kitchenBuilder.website(details.website.toString());
                }
            }
        }
        return createKitchenInternal(dto, kitchenBuilder, configBuilder,
                locationSource, resolvedFacebookId, creator);

    }


    private KitchenDto createKitchenInternal(KitchenCreateDto dto,
                                             Kitchen.KitchenBuilder kitchenBuilder,
                                             KitchenConfiguration.KitchenConfigurationBuilder configBuilder,
                                             LocationSource locationSource,
                                             String resolvedFacebookId,
                                             String creator) {

        String kitchenId  = IDUtil.generateKitchenId();
        List<MenuConfiguration> menuConfigurations = validateAndMapMenuConfigurations(kitchenId,
                dto.getMenuConfigurations(),
                dto.getFbPageId(), resolvedFacebookId);

        Date now = new Date();
        Kitchen kitchen = kitchenBuilder
                .kitchenId(kitchenId)
                .isDisabled(false)
                .createdTimestamp(now.getTime())
                .createdBy(creator).build();

        String signature = StringUtils.isNotBlank(dto.getMenuSignatureText())
                ? TextUtil.toLanguagesLowerCase(dto.getMenuSignatureText(), dto.getLanguages())
                : null;
        String lineDelimiter =  StringUtils.isNotBlank(dto.getLineDelimiter())
                ? dto.getLineDelimiter()
                : Defaults.LINE_DELIMITER_REGEX;
        String wordDelimiter =  StringUtils.isNotBlank(dto.getWordDelimiter())
                ? dto.getWordDelimiter()
                : Defaults.WORD_DELIMITER_REGEX;

        KitchenConfiguration kitchenConfiguration =  configBuilder
                .kitchenId(kitchen.getKitchenId())
                .createdTimestamp(now.getTime())
                .ignoreStopWords(false)
                .isDisabled(false)
                .languages(dto.getLanguages())
                .menuSignatureText(signature)
                .primaryLocationSource(locationSource)
                .useBruteForceMatchIfNecessary(true)
                .lineDelimiterRegex(lineDelimiter)
                .wordDelimiterRegex(wordDelimiter)
                .build();
        KitchenDto kitchenDto =  saveKitchen(kitchen, kitchenConfiguration, menuConfigurations, null);
        ResilienceUtil.retryOnExceptionSilently(() -> eventService.publishMenuEvents(kitchenDto.getId(), menuConfigurations));
        return kitchenDto;
    }

    //TODO: Menu Examples to sagemaker.
    @SneakyThrows
    public KitchenDto updateKitchen (KitchenUpdateDto dto, String kitchenId, String updater) {
        Optional<Kitchen> optional = kitchenDao.getKitchenByKitchenId(kitchenId);
        if (!optional.isPresent() || optional.get().getIsDisabled()){
            throw new NotFoundException("Kitchen not found");
        }
        Kitchen kitchen = optional.get();
        KitchenConfiguration configuration = kitchenConfigurationDao
                .getActiveKitchenConfiguration(kitchenId)
                .orElseThrow(() -> new RuntimeException("Kitchen configuration not found."));

        LocationSource locationSource = validateAndResolveLocationSource(
                dto.getPrimaryLocationSource(),
                dto.getPlacesId(),
                dto.getFbPageId()
        );

        String resolvedFacebookId = kitchen.getFacebookId();

        if (StringUtils.isNotBlank(dto.getFbPageId())) {
            if (!dto.getFbPageId().equals(kitchen.getFacebookId())) {
                //Might be a resolved facebook id, validate to prevent calling facebook
                validateFacebookId(dto.getFbPageId());
                FacebookPageDto page = resolveFacebookPage(dto.getFbPageId());
                resolvedFacebookId = page.getId();
                if (!resolvedFacebookId.equals(kitchen.getFacebookId())) {
                    //Validate to prevent existing duplicate
                    validateFacebookId(resolvedFacebookId);
                    kitchen.setFacebookId(resolvedFacebookId);
                    kitchen.setPageUrl(FACEBOOK_HOST + resolvedFacebookId);
                    if (locationSource == LocationSource.FACEBOOK_PAGE) {
                        kitchen.setDefaultDisplayName(page.getName());
                        kitchen.setDefaultDisplayAddress(page.getSingleLineAddress());
                        kitchen.setWebsite(page.getWebsite());
                    }
                }
            }
        } else {
            kitchen.setFacebookId(null);
            kitchen.setPageUrl(null);
            if (locationSource == LocationSource.FACEBOOK_PAGE) {
                kitchen.setDefaultDisplayName(null);
                kitchen.setDefaultDisplayAddress(null);
                kitchen.setWebsite(null);
            }
        }

        if (StringUtils.isNotBlank(dto.getPlacesId())) {
            if (!dto.getPlacesId().equals(kitchen.getGooglePlacesId()))  {
                PlaceDetails details = resolveGooglePlaces(dto.getPlacesId()); //Same ID expected
                String resolvedPlacesId = details.placeId;
                validateGooglePlaces(resolvedPlacesId);
                kitchen.setGooglePlacesId(resolvedPlacesId);
                if (locationSource == LocationSource.GOOGLE_PLACES) {
                    kitchen.setDefaultDisplayName(details.name);
                    kitchen.setDefaultDisplayAddress(details.formattedAddress);
                    if (details.website != null) {
                        kitchen.setWebsite(details.website.toString());
                    }
                }
            }
        } else  {
            kitchen.setGooglePlacesId(null);
            if (locationSource == LocationSource.GOOGLE_PLACES) {
                kitchen.setDefaultDisplayName(null);
                kitchen.setDefaultDisplayAddress(null);
                kitchen.setWebsite(null);
            }

        }

        List<MenuConfiguration> newConfigs = validateAndMapMenuConfigurations(kitchenId, dto.getMenuConfigurations(),
                dto.getFbPageId(), resolvedFacebookId);

        Set<String> newConfigIds = newConfigs.stream()
                .map(MenuConfiguration::getId)
                .collect(Collectors.toSet());

        List<MenuConfiguration> toCleanUpMenus = menuConfigurationDao.getMenuConfigurationsForKitchen(kitchenId)
                .stream()
                .filter(config -> !newConfigIds.contains(config.getId()))
                .collect(Collectors.toList());

        Date now = new Date();
        kitchen.setUpdatedTimestamp(now.getTime());
        kitchen.setLastUpdatedBy(updater);

        configuration.setLanguages(dto.getLanguages());
        configuration.setLineDelimiterRegex(dto.getLineDelimiter());
        configuration.setWordDelimiterRegex(dto.getWordDelimiter());
        configuration.setPrimaryLocationSource(locationSource);

        String signature = StringUtils.isNotBlank(dto.getMenuSignatureText())
                ? TextUtil.toLanguagesLowerCase(dto.getMenuSignatureText(), dto.getLanguages())
                : null;
        configuration.setMenuSignatureText(signature);
        KitchenDto result = saveKitchen(kitchen, configuration, newConfigs, toCleanUpMenus);
        cleanupOldMenuConfigs(kitchenId, toCleanUpMenus);
        ResilienceUtil.retryOnException(() -> eventService.publishMenuEvents(kitchenId, newConfigs));
        return result;
    }

    private void cleanupOldMenuConfigs(String kitchenId, List<MenuConfiguration> configs) {
        if (!configs.isEmpty()) {
            queueService.enqueueJanitorEventMessages(
                    configs.stream()
                            .map(c -> JanitorEvent.
                                    builder()
                                    .type(JanitorEventType.MENUS_BY_MENU_CONFIG)
                                    .kitchenId(kitchenId)
                                    .value(c.getId())
                                    .build())
                            .collect(Collectors.toList())
            );
        }
    }

    public KitchenDto fetchKitchen(String kitchenId) {
        Optional<Kitchen> optional = kitchenDao.getKitchenByKitchenId(kitchenId);
        if (!optional.isPresent() || optional.get().getIsDisabled()){
            throw new NotFoundException("Kitchen not found");
        }
        return mapKitchenDetails(optional.get());
    }

    public KitchenDto fetchKitchenByPlacesId(String placesId) {
        List<Kitchen> result = kitchenDao.getKitchensByPlacesId(placesId);
        if (result.isEmpty()){
            throw new NotFoundException("Kitchen not found for placesId " + placesId);
        }
        return mapKitchenDetails(result.get(0));
    }

    public KitchenDto fetchKitchenByFacebookPageId(String pageId) {
        List<Kitchen> result = kitchenDao.getKitchensByFacebookId(pageId);
        if (result.isEmpty()){
            throw new NotFoundException("Kitchen not found for facebook id " + pageId);
        }
        return mapKitchenDetails(result.get(0));
    }

    private KitchenDto mapKitchenDetails(Kitchen kitchen) {
        KitchenConfiguration configuration = kitchenConfigurationDao
                .getActiveKitchenConfiguration(kitchen.getKitchenId())
                .orElseThrow(() -> new RuntimeException("Kitchen configuration not found."));

        return kitchenMapper.toKitchenDto(kitchen, configuration,
                menuConfigurationDao.getMenuConfigurationsForKitchen(kitchen.getKitchenId()));
    }


    public void deleteKitchen(String kitchenId) {
        kitchenDao.delete(Kitchen.builder().kitchenId(kitchenId).build());
        kitchenConfigurationDao
                .getKitchenConfiguration(kitchenId, false)
                .ifPresent(kitchenConfigurationDao::delete);
        queueService.enqueueJanitorEventMessages(Collections.singletonList(
                JanitorEvent.builder()
                        .kitchenId(kitchenId)
                        .value(kitchenId)
                        .type(JanitorEventType.KITCHEN)
                        .build()));
    }


    public void reprocessMenusForKitchen(String kitchenId) {
        eventService.publishMenuEvents(kitchenId, menuConfigurationDao.getMenuConfigurationsForKitchen(kitchenId));
    }

    private KitchenDto saveKitchen(Kitchen kitchen, KitchenConfiguration configuration,
                                   List<MenuConfiguration> toSave, List<MenuConfiguration> toDelete) {
        elasticSearchService.putInKitchenIndex(kitchen, kitchen.getKitchenId());
        elasticSearchService.putInKitchenConfIndex(configuration, kitchen.getKitchenId());
        kitchenDao.saveKitchenWithConfigurations(kitchen, configuration, toSave, toDelete);
        return kitchenMapper.toKitchenDto(kitchen, configuration, toSave);
    }


    private List<MenuConfiguration> validateAndMapMenuConfigurations(String kitchenId,
            Collection<MenuConfigurationDto> dtos, String facebookId, String resolvedFacebookId) {
        Map<MenuConfigurationType, List<MenuConfigurationDto>> grouping = dtos.stream()
                .collect(Collectors.groupingBy(MenuConfigurationDto::getType));

        //Validate Facebook MenuConfig
        if (grouping.containsKey(MenuConfigurationType.FACEBOOK_PAGE)) {
            if (grouping.get(MenuConfigurationType.FACEBOOK_PAGE).size() > 1
                    || (!grouping.get(MenuConfigurationType.FACEBOOK_PAGE).get(0).getValue().equals(facebookId)
                                    && !grouping.get(MenuConfigurationType.FACEBOOK_PAGE).get(0).getValue().equals(resolvedFacebookId))) {
                throw new InvalidRequestException("Invalid FACEBOOK_PAGE menu configuration. " +
                        "Only one configuration allowed and value must match kitchen facebook id");
            }
            //Override value with resolved id
            grouping.get(MenuConfigurationType.FACEBOOK_PAGE).get(0).setValue(resolvedFacebookId);
        }

        long timestamp = new Date().getTime();
        return grouping.values().stream()
                .flatMap(Collection::stream)
                .map(m -> MenuConfiguration
                        .builder()
                        .type(m.getType())
                        .value(m.getValue())
                        .kitchenId(kitchenId)
                        .createdTimeStamp(timestamp)
                        .id(IDUtil.generateMenuConfigId(m.getType(), m.getValue()))
                        .build())
                .collect(Collectors.toList());
    }

    private void validateFacebookId(String fbPageId) {

        if (kitchenDao.getKitchensByFacebookId(fbPageId).size() > 0) {
            throw new ConflictException(String.format("Kitchen with facebook id '%s' already exists",
                    fbPageId));
        }
    }

    private FacebookPageDto resolveFacebookPage(String fbPageId) {
        return facebookClient.getFacebookPageWithPosts(
                facebookAccessToken,
                fbPageId,
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private void validateGooglePlaces(String placesId) {
        if(kitchenDao.getKitchensByPlacesId(placesId).size() > 0) {
            throw new ConflictException(String.format("Kitchen with google places id '%s' already exists",
                    placesId));
        }

    }

    @SneakyThrows
    private PlaceDetails resolveGooglePlaces(String placesId) {
        return placesApiClient.getPlaceDetails(placesId);
    }


    private LocationSource validateAndResolveLocationSource(LocationSource locationSource, String placesId, String fbPageId)  {
        if (StringUtils.isAllBlank(fbPageId, placesId)) {
            throw new InvalidRequestException("Facebook Page or Google Places listing required");
        }
        if (LocationSource.GOOGLE_PLACES.equals(locationSource) && StringUtils.isBlank(placesId)) {
            throw new InvalidRequestException("Google place id required for location source " + locationSource);
        }

        if (LocationSource.FACEBOOK_PAGE.equals(locationSource) && StringUtils.isBlank(fbPageId)) {
            throw new InvalidRequestException("Facebook page id required for location source " + locationSource);
        }

        if (locationSource == null) {
            //Google places first as default location source
            locationSource = StringUtils.isNotBlank(placesId)
                    ? LocationSource.GOOGLE_PLACES
                    : LocationSource.FACEBOOK_PAGE;
        }
        return locationSource;
    }

}
