package io.kindx.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static io.kindx.entity.MenuGeoHash.TABLE_NAME;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDBTable(tableName = TABLE_NAME)
public class MenuGeoHash {

    public static final String TABLE_NAME = "menu_geo_hashes";
    public static final String HASH_KEY_ATTR_NAME = "hash_key";
    public static final String GEO_HASH_ATTR_NAME = "geo_hash";
    public static final String GEO_JSON_ATTR_NAME = "geo_json";
    public static final String KITCHEN_ID_ATTR_NAME = "kitchen_id";
    public static final String MENU_ID_ATTR_NAME = "menu_id";

    public static final String MENU_ID_INDEX = "kitchens.google_places_id.index";

    @DynamoDBAttribute(attributeName = HASH_KEY_ATTR_NAME)
    private String hashKey;

    @DynamoDBRangeKey(attributeName = MENU_ID_ATTR_NAME)
    private String menuId;

    @DynamoDBAttribute(attributeName = GEO_HASH_ATTR_NAME)
    private String geoHash;

    @DynamoDBAttribute(attributeName = GEO_JSON_ATTR_NAME)
    private String geoJson;

    @DynamoDBAttribute(attributeName = KITCHEN_ID_ATTR_NAME)
    private String kitchenId;

}
