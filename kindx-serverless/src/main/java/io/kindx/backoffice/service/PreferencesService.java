package io.kindx.backoffice.service;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.kindx.backoffice.dto.events.PreferencesEvent;
import io.kindx.backoffice.dto.events.UserMenuNotificationEvent;
import io.kindx.backoffice.processor.menu.MenuTextProcessor;
import io.kindx.backoffice.processor.menu.es.ESMenuProcessor;
import io.kindx.backoffice.processor.menu.es.ESMenuTextProcessor;
import io.kindx.constants.Defaults;
import io.kindx.dao.KitchenConfigurationDao;
import io.kindx.dao.MenuDao;
import io.kindx.dao.MenuFoodItemDao;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.KitchenConfiguration;
import io.kindx.entity.Menu;
import io.kindx.entity.MenuFoodItem;
import io.kindx.exception.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class PreferencesService {

    private static final Logger logger = LogManager.getLogger(PreferencesService.class);

    private float acceptableScoreThreshold;
    private ElasticSearchService elasticSearchService;
    private MenuDao menuDao;
    private KitchenConfigurationDao kitchenConfigurationDao;
    private MenuFoodItemDao menuFoodItemDao;
    private ESMenuProcessor textProcessor;
    private QueueService queueService;

    @Inject
    public PreferencesService(@Named("notificationThreshold") float acceptableScoreThreshold,
                              ElasticSearchService elasticSearchService,
                              MenuDao menuDao,
                              KitchenConfigurationDao kitchenConfigurationDao,
                              MenuFoodItemDao menuFoodItemDao,
                              QueueService queueService) {
        this.acceptableScoreThreshold = acceptableScoreThreshold;
        this.elasticSearchService = elasticSearchService;
        this.menuDao = menuDao;
        this.kitchenConfigurationDao = kitchenConfigurationDao;
        this.menuFoodItemDao = menuFoodItemDao;
        this.queueService = queueService;
        this.textProcessor = new ESMenuTextProcessor(elasticSearchService);
    }

    public void processPreferencesEvent(PreferencesEvent event) {
        if (event.getType() == PreferencesEvent.Type.MENU) {
            processMenuPreferenceEvent(event);
        }

        if (event.getType() == PreferencesEvent.Type.KITCHEN) {
            processKitchenPreferencesEvent(event);
        }
    }


    private void processMenuPreferenceEvent(PreferencesEvent event) {
        if (StringUtils.isBlank(event.getMenuId()) || StringUtils.isBlank(event.getKitchenId()) ) {
            throw new IllegalArgumentException("Menu and Kitchen ID required for event processing." + event.getId());
        }

        Optional<Menu> menu = menuDao.getMenu(event.getKitchenId(), event.getMenuId());
        if (!menu.isPresent()) {
            throw new NotFoundException(String.format("No menu found for id [%s]. Event [%s]'",
                    event.getMenuId(), event.getId()));
        }

        Optional<KitchenConfiguration> configuration =
                kitchenConfigurationDao.getActiveKitchenConfiguration(event.getKitchenId());
        if (!configuration.isPresent()) {
            throw new NotFoundException(String.format("No Kitchen config found for kitchen [%s]. Event [%s]'",
                    event.getKitchenId(), event.getId()));
        }
        processMenuPreferences(event.getUserId(), menu.get(),
                event.getKitchenId(),
                textProcessor, event.getPreferences());
    }


    private void processMenuPreferences(String user,
                                        Menu menu,
                                        String kitchenId,
                                        ESMenuProcessor textProcessor,
                                        Set<String> preferences) {
        String[] preferencesArray =  preferences.toArray(new String[0]);

        //TODO: Hack to seed in userless data for aggregation and search-as-you-type. Remove
        final String userId = StringUtils.isBlank(user) ? Defaults.SYSTEM_USER_ID : user;

        Set<String> notificationFoodItems = new HashSet<>();
        Set<MenuFoodItem> menuFoodItems = new HashSet<>();

        //analyzed preferences needed to generate system names
        String[] analyzedPreferencesArray = textProcessor.analyze(preferencesArray);
        for (int i = 0; i < preferencesArray.length; i++) {
            String food = preferencesArray[i];
            List<MenuTextProcessor.ScoreResult> scores = textProcessor.scores(ESMenuProcessor.Request
                    .builder()
                    .textToScore(food)
                    .menuId(menu.getMenuId())
                    .languages(menu.getLanguages())
                    .build());

            float topScore = scores.get(0).getScore();
            String topScoreLine = scores.get(0).getLine();
            Integer topScoreLineNumber = scores.get(0).getLineNumber();

            if (topScore < acceptableScoreThreshold) {
                continue;
            }
            notificationFoodItems.add(food);

            MenuFoodItem.MenuFoodItemBuilder builder = MenuFoodItem.builder();
            String analyzedFoodText = analyzedPreferencesArray[i];
            builder.menuId(menu.getMenuId());
            builder.name(food);
            builder.userIdName(
                    String.format("%s_%s", userId,
                            food.replaceAll(" ", "_")));
            builder.systemName(analyzedFoodText);
            builder.kitchenId(kitchenId);
            builder.topScore(topScore);
            builder.topScoreLineNumber(topScoreLineNumber);
            builder.topScoreLine(topScoreLine);
            builder.scores(scores.stream().map(this::toScore).collect(Collectors.toList()));
            builder.userId(userId);
            builder.createdTimestamp(new Date().getTime());
            menuFoodItems.add(builder.build());
        }
        logger.debug("'[{}]' food item score(s) for user '[{}]' in kitchen '[{}]'. " +
                        "'[{}]' item(s) are greater than threshold",
                menuFoodItems.size(), userId, kitchenId, notificationFoodItems.size());

        if (!menuFoodItems.isEmpty()) {
            saveMenuFoodItems(menuFoodItems);
            putFoodItemsInElasticSearch(menuFoodItems, userId, menu.getMenuId());
        }
        if (!notificationFoodItems.isEmpty() && !Defaults.SYSTEM_USER_ID.equals(userId) ) {
            try {
                queueForNotification(menu.getMenuId(), kitchenId, userId, notificationFoodItems);
            } catch (Exception ex) {
                logger.error("Could notify user '{}' for kitchen '{}' and menu '{}' : {}",
                       userId, kitchenId, menu.getMenuId(), ex.getMessage(), ex);
            }
        }
    }

    private void processKitchenPreferencesEvent(PreferencesEvent event) {
        if (StringUtils.isBlank(event.getKitchenId()) ) {
            throw new IllegalArgumentException("Kitchen ID required for event processing." + event.getId());
        }
        Optional<KitchenConfiguration> configuration =
                kitchenConfigurationDao.getActiveKitchenConfiguration(event.getKitchenId());
        if (!configuration.isPresent()) {
            throw new NotFoundException(String.format("No Kitchen config found for kitchen [%s]. Event [%s]'",
                    event.getKitchenId(), event.getId()));
        }
        for (Menu menu : menuDao.getMenusForKitchen(event.getKitchenId())) {
           processMenuPreferences(
                    event.getUserId(),
                    menu,
                    event.getKitchenId(),
                    textProcessor,
                    event.getPreferences());
        }

    }

    private MenuFoodItem.Score toScore(MenuTextProcessor.ScoreResult scoreResult) {
        MenuFoodItem.Score score = new MenuFoodItem.Score();
        score.setLine(scoreResult.getLine());
        score.setLineNumber(scoreResult.getLineNumber());
        score.setScore(scoreResult.getScore());
        return score;
    }

    private void saveMenuFoodItems(Collection<MenuFoodItem> items) {
        items.forEach(i -> menuFoodItemDao.forceSave(() -> i));
    }

    private void putFoodItemsInElasticSearch(Collection<MenuFoodItem> menuFoodItems, String userId, String menuId) {
        try {
            //Put in elastic search
            elasticSearchService.putInFoodItemsIndex(
                    menuFoodItems.stream().collect(
                            Collectors.toMap(
                                    (m -> String.join("_", m.getUserIdName(), m.getMenuId())),
                                    (m -> m)
                            )));
        } catch (Exception ex) {
            logger.error("Could not put food items to elasticsearch: User: '{}', Menu: '{}'", userId, menuId);
        }
    }

    private void queueForNotification(String menuId,
                                      String kitchenId,
                                      String userId,
                                      Collection<String> notificationFoodItems) {
        UserMenuNotificationEvent notification = new UserMenuNotificationEvent();
        notification.setFoodItems(notificationFoodItems);
        notification.setKitchenId(kitchenId);
        notification.setMenuId(menuId);
        notification.setUserId(userId);
        queueService.enqueueUserMessageNotification(notification);
    }

}
