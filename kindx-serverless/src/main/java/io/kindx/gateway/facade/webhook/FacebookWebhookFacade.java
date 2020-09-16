package io.kindx.gateway.facade.webhook;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.kindx.backoffice.service.QueueService;
import io.kindx.dto.facebook.webhook.FacebookWebhookEventChangeDto;
import io.kindx.dto.facebook.webhook.FacebookWebhookEventDto;
import io.kindx.dto.facebook.webhook.FacebookWebhookEventEntryDto;
import io.kindx.gateway.exception.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FacebookWebhookFacade {
    private static final String VERIFY_MODE_FIELD = "hub.mode";
    private static final String VERIFY_CHALLENGE_FIELD = "hub.challenge";
    private static final String VERIFY_TOKEN_FIELD = "hub.verify_token";
    private static final String EVENT_TYPE_PAGE = "page";
    private static final String CHANGE_TYPE_MENTION = "mention";

    private static final Logger logger = LogManager.getLogger(FacebookWebhookFacade.class);

    private String facebookWebHookVerificationToken;
    private QueueService queueService;

    @Inject
    public FacebookWebhookFacade(@Named("facebookWebhookVerifyToken")String facebookWebHookVerificationToken,
                                 QueueService queueService) {
        this.facebookWebHookVerificationToken = facebookWebHookVerificationToken;
        this.queueService = queueService;
    }


    public String verifyWebhook(Map<String, String> params) {
        String token = params.get(VERIFY_TOKEN_FIELD);
        String mode = params.get(VERIFY_MODE_FIELD);
        String challenge = params.get(VERIFY_CHALLENGE_FIELD);
        if (!StringUtils.isNoneBlank(token, mode, challenge)
                || !"subscribe".equals(mode)
                || !facebookWebHookVerificationToken.equals(token) ) {
            logger.warn("Failed FB webhook verification. Token: [{}], Mode: [{}], Challenge: [{}]",
                    token, mode, challenge);
            throw new InvalidRequestException("Invalid webhook verification request");
        }
        return challenge;
    }


    public void processWebhookEvent(FacebookWebhookEventDto event) {
        if (EVENT_TYPE_PAGE.equals(event.getObject())) {
            List<FacebookWebhookEventChangeDto> mentionChanges = getMentionEventChanges(event);
            queueService.enqueueFacebookWebhookChangesMessages(mentionChanges);
        }
    }

    private List<FacebookWebhookEventChangeDto> getMentionEventChanges(FacebookWebhookEventDto event) {
        return  event.getEntry()
                .stream()
                .map(FacebookWebhookEventEntryDto::getChanges)
                .flatMap(List::stream)
                .filter(e -> CHANGE_TYPE_MENTION.equals(e.getField()))
                .collect(Collectors.toList());
    }
}
