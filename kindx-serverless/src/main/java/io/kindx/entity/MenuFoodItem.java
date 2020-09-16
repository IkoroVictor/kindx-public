package io.kindx.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static io.kindx.entity.MenuFoodItem.TABLE_NAME;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDBTable(tableName = TABLE_NAME)
public class MenuFoodItem {

    public static final String TABLE_NAME = "menu_food_items";
    public static final String MENU_ID_ATTR_NAME = "menu_id";
    public static final String KITCHEN_ID_ATTR_NAME = "kitchen_id";
    public static final String USER_ID_ATTR_NAME = "user_id";
    public static final String USER_ID_NAME_ATTR_NAME = "user_id_name";
    public static final String SYSTEM_NAME_ATTR_NAME = "system_name";
    public static final String USER_MENU_SYSTEM_NAME_INDEX = "menu_food_items.user_id-menu_id-system_name.index";
    public static final String USER_KITCHEN_INDEX = "menu_food_items.user_id-kitchen_id-user_id_name.index";

    @DynamoDBHashKey(attributeName = MENU_ID_ATTR_NAME)
    private String menuId;

    @DynamoDBRangeKey(attributeName = USER_ID_NAME_ATTR_NAME)
    private String userIdName;

    @DynamoDBAttribute(attributeName = USER_ID_ATTR_NAME)
    private String userId;

    @DynamoDBAttribute(attributeName = KITCHEN_ID_ATTR_NAME)
    private String kitchenId;

    @DynamoDBAttribute(attributeName = "name")
    private String name;

    @DynamoDBAttribute(attributeName = "top_score")
    private Float topScore;

    @DynamoDBAttribute(attributeName = "top_score_line")
    private String topScoreLine;

    @DynamoDBAttribute(attributeName = "top_score_line_number")
    private Integer topScoreLineNumber;

    @DynamoDBAttribute(attributeName = "scores")
    private List<Score> scores;

    @DynamoDBAttribute(attributeName = SYSTEM_NAME_ATTR_NAME)
    private String systemName;

    @DynamoDBAttribute(attributeName = "created_timestamp")
    private Long createdTimestamp;


    @Data
    @DynamoDBDocument
    public static class Score  {
        @DynamoDBAttribute(attributeName = "score")
        private float score;

        @DynamoDBAttribute(attributeName = "line_number")
        private int lineNumber;

        @DynamoDBAttribute(attributeName = "line")
        private String line ;
    }
}
