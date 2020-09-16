package io.kindx.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static io.kindx.entity.FoodItemAggregation.TABLE_NAME;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDBTable(tableName = TABLE_NAME)
public class FoodItemAggregation {

    public static final String TABLE_NAME = "food_items_aggregations";
    public static final String SYSTEM_NAME_ATTR_NAME = "system_name";
    public static final String MENU_ID_ATTR_NAME = "menu_id";

    @DynamoDBHashKey(attributeName = MENU_ID_ATTR_NAME)
    private String menuId;

    @DynamoDBRangeKey(attributeName = SYSTEM_NAME_ATTR_NAME)
    private String systemName;

    @DynamoDBAttribute(attributeName = "name")
    private String name;

    @DynamoDBAttribute(attributeName = "count")
    private Long count;

    @DynamoDBAttribute(attributeName = "update_timestamp")
    private Long updatedTimestamp;

    @DynamoDBVersionAttribute
    private Long version;
}
