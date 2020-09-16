package io.kindx.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import io.kindx.backoffice.processor.notification.NotificationChannel;
import io.kindx.backoffice.processor.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

import static io.kindx.entity.UserNotification.TABLE_NAME;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = TABLE_NAME)
public class UserNotification {

    public static final String TABLE_NAME = "users_notifications";
    public static final String KITCHEN_MENU_ID_ATTR_NAME = "kitchen_menu_id";
    public static final String USER_ID_ATTR_NAME = "user_id";

    @DynamoDBHashKey(attributeName = KITCHEN_MENU_ID_ATTR_NAME)
    private String kitchenMenuId;

    @DynamoDBRangeKey(attributeName = USER_ID_ATTR_NAME)
    private String userId;

    @DynamoDBAttribute(attributeName = "menu_id")
    private String menuId;

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "channel")
    private NotificationChannel channel;

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "type")
    private NotificationType type;

    @DynamoDBAttribute(attributeName = "user_channel_identity")
    private String userChannelIdentity;

    @DynamoDBAttribute(attributeName = "kitchen_id")
    private String kitchenId;

    @DynamoDBAttribute(attributeName = "notification_date")
    private Date date;

    @DynamoDBVersionAttribute
    private Long version;


}
