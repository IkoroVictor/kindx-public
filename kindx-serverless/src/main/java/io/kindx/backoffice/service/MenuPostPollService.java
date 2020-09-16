package io.kindx.backoffice.service;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.kindx.client.FacebookClient;
import io.kindx.dto.facebook.FacebookPageDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MenuPostPollService {

    private static final Logger logger = LogManager.getLogger(MenuPostPollService.class);

    private String facebookAccessToken;
    private FacebookClient facebookClient;
    private long postTimeWindowInSeconds;
    private QueueService queueService;

    @Inject
    public MenuPostPollService(FacebookClient facebookClient,
                               QueueService queueService,
                               @Named("facebookToken") String facebookToken,
                               @Named("postTimeWindowInSeconds") long postTimeWindowInSeconds) {
        this.facebookClient = facebookClient;
        this.postTimeWindowInSeconds = postTimeWindowInSeconds;
        this.facebookAccessToken  = facebookToken;
        this.queueService = queueService;

    }

    public void pollAndEnqueueTodayPosts(Set<String> pageUsernames) {
        try {
            List<FacebookPageDto> pageWithPosts = pageUsernames
                    .stream()
                    .map(this::mapToFacebookPageWithPosts)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (pageWithPosts.isEmpty()) {
                logger.info("No menu posts for [{}]......skipping.", String.join(",", pageUsernames));
                return;
            }
            queueService.enqueueFacebookPagePosts(pageWithPosts);
        } catch (Exception ex) {
            logger.error("Error polling today's posts for kitchens ['{}'], Skipping..... Cause: {}",
                    String.join(", ", pageUsernames), ex.getMessage(), ex);
        }
    }

    private FacebookPageDto mapToFacebookPageWithPosts(String pageUsername) {
        FacebookPageDto pageWithPostsDto = null;
        long sinceUnixTimestamp = LocalDateTime.now()
                .minusSeconds(postTimeWindowInSeconds)
                .toInstant(ZoneOffset.UTC)
                .getEpochSecond();
        try {
            pageWithPostsDto = facebookClient.getFacebookPageWithPosts(facebookAccessToken,
                    pageUsername, String.valueOf(sinceUnixTimestamp));
        } catch (Exception ex) {
            logger.error("Error mapping page posts for page username '{}', Skipping..... Cause: {}", pageUsername, ex.getMessage(), ex);
        }
        return pageWithPostsDto;
    }



}
