package io.kindx.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static io.kindx.entity.Kitchen.KITCHEN_TABLE_NAME;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = KITCHEN_TABLE_NAME)
public class Kitchen {

    public static final String KITCHEN_TABLE_NAME = "kitchens";
    public static final String KITCHEN_FACEBOOK_INDEX = "kitchens.facebook_id.index";
    public static final String KITCHEN_PLACES_INDEX = "kitchens.google_places_id.index";
    public static final String LAST_JOB_TIMESTAMP_ATTR_NAME = "last_job_timestamp";
    public static final String KITCHEN_ID_ATTR_NAME = "kitchen_id";
    public static final String FACEBOOK_ID_ATTR_NAME = "facebook_id";
    public static final String GOOGLE_PLACES_ID = "google_places_id";

    @DynamoDBHashKey(attributeName = KITCHEN_ID_ATTR_NAME)
    private String kitchenId;

    @DynamoDBAttribute(attributeName = FACEBOOK_ID_ATTR_NAME)
    private String facebookId;

    @DynamoDBAttribute(attributeName = GOOGLE_PLACES_ID)
    private String googlePlacesId;

    @DynamoDBAttribute(attributeName = "created_timestamp")
    private Long createdTimestamp;

    @DynamoDBAttribute(attributeName = "created_by")
    private String createdBy;

    @DynamoDBAttribute(attributeName = "page_url")
    private String pageUrl;

    @DynamoDBAttribute(attributeName = "website")
    private String website;

    @DynamoDBAttribute(attributeName = "default_display_name")
    private String defaultDisplayName;

    @DynamoDBAttribute(attributeName = "default_display_address")
    private String defaultDisplayAddress;

    @DynamoDBAttribute(attributeName = "fallback_thumbnail_url")
    private String fallbackThumbnailUrl;

    @DynamoDBAttribute(attributeName = "is_disabled")
    private Boolean isDisabled;

    @DynamoDBAttribute(attributeName = "last_job_timestamp")
    private Long lastJobTimestamp;

    @DynamoDBAttribute(attributeName = "updated_timestamp")
    private Long updatedTimestamp;

    @DynamoDBAttribute(attributeName = "last_updated_by")
    private String lastUpdatedBy;

    @DynamoDBVersionAttribute
    private Long version;
}
