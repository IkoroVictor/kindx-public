package io.kindx.entity;


import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import static io.kindx.entity.UserKitchenMapping.TABLE_NAME;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDBTable(tableName = TABLE_NAME)
public class UserKitchenMapping {

    public static final String TABLE_NAME = "users_kitchens";
    public static final String KITCHEN_ID_ATTR_NAME = "kitchen_id";
    public static final String USER_ID_ATTR_NAME = "user_id";
    public static final String FOOD_PREFERENCES_ATTR_NAME = "food_preferences";

    public static final String USER_ID_INDEX = "users_kitchens.user_id.index";

    @DynamoDBHashKey(attributeName = "kitchen_id")
    private String kitchenId;

    @DynamoDBRangeKey(attributeName = "user_id")
    private String userId;

    @DynamoDBAttribute(attributeName = FOOD_PREFERENCES_ATTR_NAME)
    private Set<String> foodPreferences;

    @DynamoDBAttribute(attributeName = "created_timestamp")
    private Long createdTimestamp;


    @DynamoDBAttribute(attributeName = "is_disabled")
    private Boolean isDisabled;

    @DynamoDBAttribute(attributeName = "should_notify")
    private Boolean shouldNotify;

    @DynamoDBVersionAttribute
    private Long version;


}
