package io.kindx.backoffice.service;

import com.google.inject.Inject;
import io.kindx.backoffice.dto.events.UserMenuNotificationEvent;
import io.kindx.backoffice.processor.notification.NotificationProcessor;
import io.kindx.backoffice.processor.template.TemplateProcessor;
import io.kindx.dao.UserNotificationDao;
import io.kindx.entity.UserNotification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserNotificationService {
    private static final Logger logger = LogManager.getLogger(UserNotificationService.class);
    private TemplateProcessor templateProcessor;
    private NotificationProcessor notificationProcessor;
    private UserNotificationDao notificationDao;

    @Inject
    public UserNotificationService(TemplateProcessor templateProcessor,
                                   NotificationProcessor notificationProcessor,
                                   UserNotificationDao notificationDao) {
        this.templateProcessor = templateProcessor;
        this.notificationProcessor = notificationProcessor;
        this.notificationDao = notificationDao;
    }

    public void processUserMenuNotification (UserMenuNotificationEvent notification) {
        String kitchenMenuId = String.format("%s|%s", notification.getKitchenId(), notification.getMenuId());
        if (notificationDao.notificationExists(kitchenMenuId, notification.getUserId())) {
            logger.info("Notification '{}' already exists for user '{}'.......skipping",
                    kitchenMenuId,
                    notification.getUserId());
            return;
        }

        try {
           notificationDao.saveNotification(mapToNotification(kitchenMenuId, notification));
        } catch (Exception ex) {
           logger.error("Could not process notification, {} . User: '{}', Menu: '{}', Kitchen : '{};",
                   ex.getMessage(),
                   notification.getUserId(),
                   notification.getMenuId(),
                   notification.getKitchenId(),
                   ex
           );
       }
    }

    private UserNotification mapToNotification(String kitchenMenuId,
                                               UserMenuNotificationEvent dto){
        return UserNotification
                .builder()
                .kitchenId(dto.getKitchenId())
                .menuId(dto.getMenuId())
                .userId(dto.getUserId())
                .kitchenMenuId(kitchenMenuId)
                .build();
    }
}
