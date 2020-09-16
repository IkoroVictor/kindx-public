package io.kindx.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import io.kindx.constants.MenuConfigurationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static io.kindx.entity.MenuConfiguration.TABLE_NAME;

@Data
@DynamoDBDocument
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = TABLE_NAME)
public class MenuConfiguration {

    public static final String TABLE_NAME = "menu_configurations";
    public static final String KITCHEN_ID_ATTR_NAME = "kitchen_id";
    public static final String ID_ATTR_NAME = "id";
    public static final String TYPE_ATTR_NAME = "type";
    public static final String VALUE_ATTR_NAME = "value";
    public static final String CREATED_TIMESTAMP_ATTR_NAME = "created_timestamp";
    public static final String TYPE_INDEX = "menu_configurations.type.index";


    @DynamoDBHashKey(attributeName = KITCHEN_ID_ATTR_NAME)
    private String kitchenId;

    @DynamoDBRangeKey(attributeName = ID_ATTR_NAME)
    private String id;

    @DynamoDBAttribute(attributeName = TYPE_ATTR_NAME)
    @DynamoDBTypeConvertedEnum
    private MenuConfigurationType type;

    @DynamoDBAttribute(attributeName = VALUE_ATTR_NAME)
    private String value;

    @DynamoDBAttribute(attributeName = CREATED_TIMESTAMP_ATTR_NAME )
    private Long createdTimeStamp;


}
