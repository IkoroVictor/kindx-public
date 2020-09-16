package io.kindx.backoffice.service;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.kindx.backoffice.dto.events.FacebookTaggedPostEvent;
import io.kindx.client.FacebookClient;
import io.kindx.constants.Defaults;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.MenuConfigurationDao;
import io.kindx.dto.facebook.FacebookPageDto;
import io.kindx.dto.facebook.FacebookPostDto;
import io.kindx.entity.Kitchen;
import io.kindx.entity.MenuConfiguration;
import io.kindx.util.IDUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FacebookPollService {

    private static final Logger logger = LogManager.getLogger(FacebookPollService.class);
    private static final String LOG_FORMAT = "[EventId: %s] [Message: %s]";

    private long postTimeWindowInSeconds;
    private QueueService queueService;
    private String facebookToken;
    private FacebookClient client;
    private KitchenDao kitchenDao;
    private MenuConfigurationDao menuConfigurationDao;

    @Inject
    public FacebookPollService(@Named("facebookToken") String facebookToken,
                               @Named("postTimeWindowInSeconds") long postTimeWindowInSeconds,
                               FacebookClient client,
                               KitchenDao kitchenDao,
                               QueueService queueService,
                               MenuConfigurationDao menuConfigurationDao) {
        this.postTimeWindowInSeconds = postTimeWindowInSeconds;
        this.queueService = queueService;
        this.facebookToken = facebookToken;
        this.client = client;
        this.kitchenDao = kitchenDao;
        this.menuConfigurationDao = menuConfigurationDao;
    }

    public Map pollFacebookTaggedPostEvents(String eventId) {
        String sinceUnixTimestamp = String.valueOf(LocalDateTime.now()
                .minusSeconds(postTimeWindowInSeconds)
                .toInstant(ZoneOffset.UTC)
                .getEpochSecond());

        FacebookPageDto.PagePosts taggedPosts = client.getTaggedPosts(facebookToken,
                Defaults.KINDX_APP_FACEBOOK_PAGE_USERNAME,
                sinceUnixTimestamp, "");

        logger.info(String.format(LOG_FORMAT, eventId, "{} tagged posts found..."),
                taggedPosts.getPosts().size());
        int failed = 0;
        int total = 0;
        do {
            List<FacebookPostDto> posts = taggedPosts.getPosts();
            total += posts.size();
            try {
                List<FacebookTaggedPostEvent> events = filterAndMapToEvents(posts);
                queueService.enqueueFacebookTaggedPostEventMessages(events);
                failed += (posts.size() - events.size());
            } catch (Exception ex) {
                logger.error("Error publishing facebook tagged posts events  - {}",  ex.getMessage(), ex);
                failed += posts.size();
            }
            if (taggedPosts.getPaging() != null) {
                taggedPosts = client.getTaggedPosts(facebookToken,
                        Defaults.KINDX_APP_FACEBOOK_PAGE_USERNAME,
                        sinceUnixTimestamp,
                        taggedPosts.getPaging().getCursors().getAfter());
            }
        } while (!taggedPosts.getPosts().isEmpty() || taggedPosts.getPaging() != null);
        logger.info(String.format(LOG_FORMAT, eventId, "{} tagged posts processed............"), total);

        return ImmutableMap.of("total", total, "failed",  failed);
    }


    private List<FacebookTaggedPostEvent> filterAndMapToEvents(List<FacebookPostDto> posts) {
        List<FacebookTaggedPostEvent> events = new ArrayList<>();

        for (FacebookPostDto p : posts) {
            try {
                String pageId = p.getId().split("_")[0];
                List<Kitchen> kitchens = kitchenDao.getKitchensByFacebookId(pageId);
                if (kitchens.isEmpty()) {
                    logger.warn("Kitchen with page id '{}' not found.   Skipping...", pageId);
                    continue;
                }
                Kitchen kitchen = kitchens.get(0);
                MenuConfiguration menuConfiguration = menuConfigurationDao.getMenuConfigurationsForKitchen(kitchen.getKitchenId())
                        .stream()
                        .filter(m -> MenuConfigurationType.FACEBOOK_PAGE.equals(m.getType())).findAny().orElse(null);

                if (menuConfiguration == null) {
                    logger.info("No FACEBOOK_PAGE menu configuration found for '{}' - '{}', creating one.....",
                            kitchen.getKitchenId(), pageId);
                    menuConfiguration = MenuConfiguration.builder()
                            .id(IDUtil.generateMenuConfigId(MenuConfigurationType.FACEBOOK_PAGE, pageId))
                            .createdTimeStamp(new Date().getTime())
                            .kitchenId(kitchen.getKitchenId())
                            .type(MenuConfigurationType.FACEBOOK_PAGE)
                            .value(pageId)
                            .build();
                    menuConfigurationDao.forceSave(menuConfiguration);
                }
                events.add(FacebookTaggedPostEvent.
                        builder()
                        .post(p)
                        .facebookId(pageId)
                        .kitchenId(kitchen.getKitchenId())
                        .menuConfigurationId(menuConfiguration.getId())
                        .build());
            } catch (Exception ex) {
                logger.warn("Error processing post with id {}.....skipping", p.getId(), ex);
            }
        }
        return  events;
    }

}
