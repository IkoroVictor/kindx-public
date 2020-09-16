package io.kindx.backoffice.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.client.FacebookClient;
import io.kindx.constants.JanitorEventType;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.MenuConfigurationDao;
import io.kindx.dto.facebook.FacebookPageDto;
import io.kindx.dto.facebook.FacebookPostDto;
import io.kindx.dto.facebook.webhook.FacebookWebhookEventChangeDto;
import io.kindx.entity.Kitchen;
import io.kindx.entity.MenuConfiguration;
import io.kindx.util.IDUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class FacebookWebhookService {

    private static final Logger logger = LogManager.getLogger(FacebookWebhookService.class);

    private String facebookToken;
    private FacebookClient client;
    private KitchenDao kitchenDao;
    private MenuConfigurationDao menuConfigurationDao;
    private MenuProcessorService menuProcessorService;
    private QueueService queueService;

    private static final Set<String> SUPPORTED_MENTION_ITEMS = ImmutableSet.of( "post", "tag");

    @Inject
    public FacebookWebhookService(@Named("facebookToken") String facebookToken,
                                  FacebookClient client,
                                  KitchenDao kitchenDao,
                                  MenuConfigurationDao menuConfigurationDao,
                                  MenuProcessorService menuProcessorService,
                                  QueueService queueService) {
        this.facebookToken = facebookToken;
        this.client = client;
        this.kitchenDao = kitchenDao;
        this.menuConfigurationDao = menuConfigurationDao;
        this.menuProcessorService = menuProcessorService;
        this.queueService = queueService;

    }

    public Map processChange(FacebookWebhookEventChangeDto change) {
        if (!"mention".equals(change.getField())) {
            logger.warn("Unsupported change field '{}' : {}....dropping", change.getField(), change.toString());
            return Collections.singletonMap("processed", false);
        }
        String item = (String) change.getValue().get("item");
        if (!SUPPORTED_MENTION_ITEMS.contains(item)) {
            logger.warn("Unsupported change value item '{}' : {}....dropping", item,  change.toString());
            return Collections.singletonMap("processed", false);
        }
        String postId = (String) change.getValue().get("post_id");
        String verb = (String) change.getValue().get("verb");
        String senderId = (String) change.getValue().get("sender_id");
        senderId = StringUtils.isNotBlank(senderId) ? senderId : postId.split("_")[0];

        List<Kitchen> kitchens = kitchenDao.getKitchensByFacebookId(senderId);
        if (kitchens.size() == 0) {
            logger.warn("No kitchen found for sender '{}' : {}....dropping", senderId, change.toString());
            return ImmutableMap.of("postId", postId,
                    "senderId", senderId,
                    "verb", verb,
                    "processed", false);
        }
        Kitchen kitchen =  kitchens.get(0);
        boolean processed = true;
        switch (verb) {
            case "add":
                createMenuFromPost(kitchen.getKitchenId(), kitchen.getFacebookId(), postId);
                break;
            case "remove" :
            case "delete" :
                cleanupMenu(kitchen.getKitchenId(), postId);
                break;
            default:
                processed = false;
        }

        return ImmutableMap.of("postId", postId,
                "senderId", senderId,
                "verb", verb,
                "processed", processed);

    }

    private void createMenuFromPost(String kitchenId,
                                    String pageId,
                                    String postId) {

        List<MenuConfiguration> menuConfigs = menuConfigurationDao.getMenuConfigurationsForKitchen(kitchenId);
        MenuConfiguration fbConfig = menuConfigs.stream()
                .filter(m -> MenuConfigurationType.FACEBOOK_PAGE.equals(m.getType()))
                .findFirst().orElse(null);

        if (fbConfig == null) {
            logger.info("No FACEBOOK_PAGE menu config for kitchen {}.....creating...", kitchenId);
            List<MenuConfiguration>  newConfigList = new ArrayList<>(menuConfigs);
            fbConfig = MenuConfiguration.builder()
                    .type(MenuConfigurationType.FACEBOOK_PAGE).value(pageId)
                    .kitchenId(kitchenId)
                    .id(IDUtil.generateMenuConfigId(MenuConfigurationType.FACEBOOK_PAGE,kitchenId))
                    .createdTimeStamp(new Date().getTime())
                    .build();
            newConfigList.add(fbConfig);
            menuConfigurationDao.updateMenuConfigurations(newConfigList, Collections.emptyList());
        }

        FacebookPageDto page = client.getFacebookPage(facebookToken, pageId);
        FacebookPostDto post = client.getPageSinglePost(facebookToken, postId);
        page.setPosts(FacebookPageDto.PagePosts.builder()
                .posts(Collections.singletonList(post))
                .build());
        menuProcessorService.processFacebookPageWithPosts(page, fbConfig.getId());

    }

    private void cleanupMenu(String kitchenId, String postId) {
        String id = IDUtil.generateFacebookMenuId(kitchenId, postId);
        queueService.enqueueJanitorEventMessages(ImmutableList.of(JanitorEvent.builder()
                .kitchenId(kitchenId)
                .type(JanitorEventType.MENU)
                .menuId(id)
                .value(id)
                .build()));
    }
}
