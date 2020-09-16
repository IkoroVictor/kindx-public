package io.kindx.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import io.kindx.constants.Language;
import io.kindx.constants.MenuSource;
import io.kindx.entity.converter.LanguageSetConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Set;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDBTable(tableName = Menu.TABLE_NAME)
public class Menu {

    public static final String TABLE_NAME = "menus";
    public static final String MENU_CONFIGURATION_INDEX = "menus.menu_configuration_id.index";
    public static final String KITCHEN_ID_ATTR_NAME = "kitchen_id";
    public static final String MENU_ID_ATTR_NAME = "menu_id";
    public static final String MENU_CONFIGURATION_ID_ATTR_NAME = "menu_configuration_id";

    @DynamoDBHashKey(attributeName = KITCHEN_ID_ATTR_NAME)
    private String kitchenId;

    @DynamoDBRangeKey(attributeName = MENU_ID_ATTR_NAME)
    private String menuId;

    @DynamoDBAttribute(attributeName = "location")
    private Location location;

    @DynamoDBAttribute(attributeName = "business_profile")
    private BusinessProfile businessProfile;

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "source")
    private MenuSource source;

    @DynamoDBAttribute(attributeName = "languages")
    @DynamoDBTypeConverted(converter = LanguageSetConverter.class)
    private Set<Language> languages;

    @DynamoDBAttribute(attributeName = "created_timestamp")
    private Long createdTimestamp;

    @DynamoDBIgnore
    private String menuText;

    @DynamoDBIgnore
    private String sourceValue;

    @DynamoDBAttribute(attributeName = "menu_source_url")
    private String sourceUrl;

    @DynamoDBAttribute(attributeName = "menu_date")
    private Date menuDate;

    @DynamoDBAttribute(attributeName = "thumbnail_image_url")
    private String thumbnailImageUrl;

    @DynamoDBAttribute(attributeName = "header_image_url")
    private String headerImageUrl;

    @DynamoDBAttribute(attributeName = MENU_CONFIGURATION_ID_ATTR_NAME)
    private String menuConfigurationId;

    @DynamoDBVersionAttribute
    private Long version;


    @Data
    @DynamoDBDocument
    public static class Location {

        @DynamoDBAttribute(attributeName = "geo_point")
        private GeoPoint geoPoint;

        @DynamoDBAttribute(attributeName = "address")
        private String address;

        @DynamoDBAttribute(attributeName = "name")
        private String name;

        @DynamoDBAttribute(attributeName = "street")
        private String street;

        @DynamoDBAttribute(attributeName = "zip")
        private String zip;

        @DynamoDBAttribute(attributeName = "city")
        private String city;

        @DynamoDBAttribute(attributeName = "country")
        private String country;
    }

    @Data
    @DynamoDBDocument
    public static class GeoPoint {

        @DynamoDBAttribute(attributeName = "lat")
        private Double lat;

        @DynamoDBAttribute(attributeName = "long")
        private Double lon;
    }


    @Data
    @DynamoDBDocument
    public static class BusinessProfile {

        @DynamoDBAttribute(attributeName = "business_name")
        private String businessName;

        @DynamoDBAttribute(attributeName = "emails")
        private Set<String> emails;

        @DynamoDBAttribute(attributeName = "website")
        private String website;

        @DynamoDBAttribute(attributeName = "facebook_page_url")
        private String facebookPageUrl;

        @DynamoDBAttribute(attributeName = "location")
        private Location location;

        @DynamoDBAttribute(attributeName = "phone")
        private Set<String> phoneNumbers;

        @DynamoDBAttribute(attributeName = "opening_hours")
        private List<OpeningHour> openingHours;

        @Data
        @DynamoDBDocument
        public static final class OpeningHour {
            private int openDayOfWeek;
            private String openTime;

            private int closeDayOfWeek;
            private String closeTime;
        }

    }
}
