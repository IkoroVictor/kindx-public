package io.kindx.backoffice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.kindx.backoffice.dto.events.PreferenceReprocessEvent;
import io.kindx.backoffice.dto.events.PreferencesEvent;
import io.kindx.backoffice.dto.menu.FacebookPageProcessRequestDto;
import io.kindx.backoffice.dto.menu.FacebookPageProcessResultDto;
import io.kindx.backoffice.dto.menu.HtmlMenuProcessRequestDto;
import io.kindx.backoffice.dto.menu.PlainTextMenuProcessRequestDto;
import io.kindx.backoffice.processor.filter.BinaryClassifierTextFilterProcessor;
import io.kindx.backoffice.processor.filter.TextFilterProcessor;
import io.kindx.backoffice.processor.menu.MenuTextProcessor;
import io.kindx.client.FacebookClient;
import io.kindx.constants.Language;
import io.kindx.dao.*;
import io.kindx.dto.GeoPointDto;
import io.kindx.dto.facebook.FacebookPageDto;
import io.kindx.dto.facebook.FacebookPostDto;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.*;
import io.kindx.mapper.MenuMapper;
import io.kindx.util.IDUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MenuProcessorService {
    private static final Logger logger = LogManager.getLogger(MenuProcessorService.class);

    private final Cache<String, MenuTextProcessor> menuTextProcessorCache;

    private String facebookToken;
    private long postTimeWindowInSeconds;
    private long reprocessRadiusInMeters;
    private FacebookClient facebookClient;
    private KitchenDao kitchenDao;
    private KitchenConfigurationDao kitchenConfigurationDao;
    private UserDao userDao;
    private MenuDao menuDao;
    private UserKitchenMappingDao userKitchenMappingDao;
    private ElasticSearchService elasticSearchService;
    private ObjectMapper objectMapper;
    private MenuMapper menuMapper;
    private EventService eventService;
    private QueueService queueService;


    @Inject
    public MenuProcessorService(@Named("facebookToken") String facebookToken,
                                @Named("postTimeWindowInSeconds") long postTimeWindowInSeconds,
                                @Named("reprocessRadiusInMeters") long reprocessRadiusInMeters,
                                FacebookClient facebookClient,
                                KitchenDao kitchenDao,
                                KitchenConfigurationDao kitchenConfigurationDao,
                                UserDao userDao, MenuDao menuDao,
                                UserKitchenMappingDao userKitchenMappingDao,
                                ElasticSearchService elasticSearchService,
                                ObjectMapper objectMapper,
                                MenuMapper menuMapper,
                                EventService eventService,
                                QueueService queueService) {
        this.facebookToken = facebookToken;
        this.postTimeWindowInSeconds = postTimeWindowInSeconds;
        this.reprocessRadiusInMeters = reprocessRadiusInMeters;
        this.facebookClient = facebookClient;
        this.kitchenDao = kitchenDao;
        this.kitchenConfigurationDao = kitchenConfigurationDao;
        this.userDao = userDao;
        this.menuDao = menuDao;
        this.userKitchenMappingDao = userKitchenMappingDao;
        this.elasticSearchService = elasticSearchService;
        this.objectMapper = objectMapper;
        this.menuMapper = menuMapper;
        this.eventService = eventService;
        this.queueService = queueService;
        menuTextProcessorCache = CacheBuilder.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(10, TimeUnit.MINUTES).build();
    }

    public void processPlainTextMenu(PlainTextMenuProcessRequestDto request) {
        Kitchen kitchen = getActiveKitchen(request.getKitchenId());
        KitchenConfiguration configuration = getActiveKitchenConfiguration(request.getKitchenId());
        List<UserKitchenMapping> kitchenMappings = userKitchenMappingDao.getMappingsForKitchen(kitchen.getKitchenId());

        String menuId = IDUtil.generatePlainTextMenuId(kitchen.getKitchenId(), request.getMenuConfigurationId());
        Menu menu = menuMapper.menuFromPlainText(menuId, kitchen, configuration,
                request.getText(), new Date());
        menu.setMenuConfigurationId(request.getMenuConfigurationId());
        processMenu(menu, kitchen, kitchenMappings);
    }

    public void processHtmlMenu(HtmlMenuProcessRequestDto request) {
        Kitchen kitchen = getActiveKitchen(request.getKitchenId());
        KitchenConfiguration configuration = getActiveKitchenConfiguration(request.getKitchenId());
        List<UserKitchenMapping> kitchenMappings = userKitchenMappingDao.getMappingsForKitchen(kitchen.getKitchenId());

        //TODO: Cleanup job for older menus (cleanup event when menu configurations changes)
        String menuId = request.isPdfSource()
                ? IDUtil.generatePdfMenuId(kitchen.getKitchenId(), request.getMenuConfigurationId())
                : IDUtil.generateWebPageMenuId(kitchen.getKitchenId(), request.getMenuConfigurationId());
        Menu menu = menuMapper.menuFromHtmlText(menuId, kitchen, configuration,
                request.getStrippedText(), request.getOriginalHtml(), request.getUrl(),
                new Date(), request.isPdfSource());
        menu.setMenuConfigurationId(request.getMenuConfigurationId());
        processMenu(menu, kitchen, kitchenMappings);

    }

    public FacebookPageProcessResultDto processFacebookPage(FacebookPageProcessRequestDto requestDto) {
        try {
            FacebookPageDto pageDto = facebookClient.getFacebookPage(facebookToken, requestDto.getPageId());
            pageDto.setPosts(FacebookPageDto.PagePosts
                    .builder()
                    .posts(Collections.singletonList(requestDto.getPost()))
                    .build());
            return processFacebookPageWithPosts(pageDto, requestDto.getMenuConfigurationId());
        } catch (Exception ex) {
            //TODO: log error metrics
            logger.error("Error loading  facebook page posts for page id '{}', Skipping..... Cause: {}",
                    requestDto.getPageId(), ex.getMessage(), ex);
            throw ex;
        }
    }


    public FacebookPageProcessResultDto processFacebookPageWithPosts(FacebookPageDto pageWithPosts,
                                                                              String menuConfigurationId) {
        boolean successful = false;
        int polled = 0;
        int processed = 0;

        FacebookPageProcessResultDto.FacebookPageProcessResultDtoBuilder resultDtoBuilder =
                FacebookPageProcessResultDto.builder();

        FacebookPageDto.PagePosts postsContainer = pageWithPosts.getPosts();
        if (postsContainer != null
                && postsContainer.getPosts() != null
                && !postsContainer.getPosts().isEmpty()) {

            polled = pageWithPosts.getPosts().getPosts().size();

            List<Kitchen> kitchenList = kitchenDao.getActiveKitchensByFacebookId(pageWithPosts.getId());
            if (kitchenList.isEmpty()) {
                throw new IllegalArgumentException(String.format("Kitchen with facebook id '%s' not found", pageWithPosts.getId()));
            }
            if (kitchenList.size() > 1) { //TODO: Inconsistency due to GSI index update latency
                throw new IllegalArgumentException(String.format("Multiple Kitchens with facebook id [{%s}]. Aborting process",
                        pageWithPosts.getId()));
            }

            Kitchen kitchen = kitchenList.get(0);
            resultDtoBuilder.kitchenId(kitchen.getKitchenId());

            Optional<KitchenConfiguration> configurationOptional = kitchenConfigurationDao.getActiveKitchenConfiguration(kitchen.getKitchenId());
            if (!configurationOptional.isPresent()) {
                throw new IllegalArgumentException(String.format("Kitchen configuration for kitchen id '%s' not found", kitchen.getKitchenId()));
            }

            KitchenConfiguration configuration = configurationOptional.get();
            List<UserKitchenMapping> kitchenMappings = userKitchenMappingDao.getMappingsForKitchen(kitchen.getKitchenId());
            Language[] languages = configuration.getLanguages().toArray(new Language[0]);

            TextFilterProcessor textFilterProcessor = new BinaryClassifierTextFilterProcessor(
                    configuration.getMenuSignatureText());

            List<FacebookPostDto> menuPosts = pageWithPosts.getPosts()
                    .getPosts()
                    .stream()
                    .filter(post -> StringUtils.isNotBlank(post.getMessage()))
                    .collect(Collectors.toList());

            if (!menuPosts.isEmpty()) {
                menuPosts.forEach(menuPost -> {
                    String menuId = IDUtil.generateFacebookMenuId(kitchen.getKitchenId(), menuPost.getId());

                    if (menuDao.menuExists(kitchen.getKitchenId(), menuId)) {
                        logger.info("Facebook menu post '[{}]'  for kitchen '[{}]' processed earlier..........re-processing... ",
                                menuId, kitchen.getKitchenId());
                    }
                    Menu menu = menuMapper.menuFromFacebookPost(menuId, kitchen, configuration, menuPost, pageWithPosts);
                    menu.setMenuConfigurationId(menuConfigurationId);
                    processMenu(menu, kitchen, kitchenMappings);
                });
                processed = menuPosts.size();
                successful = true;
            } else {
                logger.info("No facebook menu posts found for kitchen '[{}]'.......", kitchen.getKitchenId());
            }
        } else {
            logger.info("No page posts for facebook page '{}'.", pageWithPosts.getName());
        }
        return resultDtoBuilder
                .menuConfigurationId(menuConfigurationId)
                .pageId(pageWithPosts.getId())
                .pageUsername(pageWithPosts.getUsername())
                .successful(successful)
                .polled(polled)
                .processed(processed)
                .build();
    }

    private void processMenu(Menu menu, Kitchen kitchen, Collection<UserKitchenMapping> kitchenMappings) {

        logger.info("Processing {} mapping(s) for kitchen '{}' for {} menu '[{}]' ...........",
                kitchenMappings.size(), kitchen.getKitchenId(), menu.getSource().name(), menu.getMenuId());

        String menuId = menu.getMenuId();
        elasticSearchService.putInMenuIndex(menu, menuId);
        menuDao.forceSave(() -> menu);

        for (UserKitchenMapping mapping : kitchenMappings) {
            Optional<User> user = userDao.getUser(mapping.getUserId());
            if (!user.isPresent()) {
                logger.warn("User '[{}]'  for mapping '[{}]' not found.....Skipping...... ",
                        mapping.getUserId(), mapping.getKitchenId());
                continue;
            }
            Set<String> ukPreferences = Objects.nonNull(mapping.getFoodPreferences())
                    ? mapping.getFoodPreferences()
                    : Collections.emptySet();

            Set<String> preferences = new HashSet<>(ukPreferences);
            if (user.get().getGeneralFoodPreferences() != null) {
                preferences.addAll(user.get().getGeneralFoodPreferences());
            }
            eventService.publishPreferencesEvent(PreferencesEvent
                    .builder()
                    .id(IDUtil.generatePreferencesId())
                    .kitchenId(mapping.getKitchenId())
                    .menuId(menuId)
                    .type(PreferencesEvent.Type.MENU)
                    .userId(mapping.getUserId())
                    .preferences(preferences)
                    .build());
        }
        queueService.enqueuePreferenceReprocessMessages(Collections.singletonList(
                PreferenceReprocessEvent
                        .builder()
                        .id(IDUtil.generateReprocessId())
                        .kitchenId(kitchen.getKitchenId())
                        .menuId(menuId)
                        .pointOfFocus(GeoPointDto.builder()
                                .lon(menu.getLocation().getGeoPoint().getLon())
                                .lat(menu.getLocation().getGeoPoint().getLat())
                                .build())
                        .searchRadiusInMeters(reprocessRadiusInMeters)
                        .type(PreferenceReprocessEvent.ReprocessType.KITCHEN_MENU_UPDATE)
                        .build()
        ));

        logger.info("{} menu  '[{}]'  for kitchen '[{}]' processed successfully.......... ",
                menu.getSource().name(), menuId, kitchen.getKitchenId());
    }


    private MenuFoodItem.Score toScore(MenuTextProcessor.ScoreResult scoreResult) {
        MenuFoodItem.Score score = new MenuFoodItem.Score();
        score.setLine(scoreResult.getLine());
        score.setLineNumber(scoreResult.getLineNumber());
        score.setScore(scoreResult.getScore());
        return score;
    }


    private Kitchen getActiveKitchen(String kitchenId) {
        //TODO: Queue event to call this method
        Optional<Kitchen> kitchenOptional = kitchenDao.getActiveKitchenByKitchenId(kitchenId);
        if (!kitchenOptional.isPresent()) {
            throw new IllegalArgumentException(String.format("Kitchen with id '%s' not found", kitchenId));
        }

        return kitchenOptional.get();
    }

    private KitchenConfiguration getActiveKitchenConfiguration(String kitchenId) {
        Optional<KitchenConfiguration> configurationOptional = kitchenConfigurationDao.
                getActiveKitchenConfiguration(kitchenId);
        if (!configurationOptional.isPresent()) {
            throw new IllegalArgumentException(String.format("Kitchen configuration for kitchen id '%s' not found",
                    kitchenId));
        }

        return configurationOptional.get();
    }

}
