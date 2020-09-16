package io.kindx.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import io.kindx.constants.Language;
import io.kindx.constants.LocationSource;
import io.kindx.entity.converter.LanguageSetConverter;
import lombok.*;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDBTable(tableName = KitchenConfiguration.TABLE_NAME)
public class KitchenConfiguration {

    public static final String TABLE_NAME = "kitchen_configurations";
    public static final String KITCHEN_ID_ATTR_NAME = "kitchen_id";
    public static final String CREATED_TIMESTAMP_ATTR_NAME = "created_timestamp";

    @DynamoDBHashKey(attributeName = KITCHEN_ID_ATTR_NAME)
    private String kitchenId;

    @DynamoDBRangeKey(attributeName = CREATED_TIMESTAMP_ATTR_NAME)
    private Long createdTimestamp;

    @DynamoDBAttribute(attributeName = "is_disabled")
    private Boolean isDisabled;

    @DynamoDBAttribute(attributeName = "brute_force_fallback")
    private Boolean useBruteForceMatchIfNecessary;

    @DynamoDBAttribute(attributeName = "ignore_stop_words")
    private Boolean ignoreStopWords;

    @DynamoDBAttribute(attributeName = "line_delimiter_regex")
    private String lineDelimiterRegex;

    @DynamoDBAttribute(attributeName = "word_delimiter_regex")
    private String wordDelimiterRegex;

    @DynamoDBAttribute(attributeName = "languages")
    @DynamoDBTypeConverted(converter = LanguageSetConverter.class)
    @Singular
    private Set<Language> languages;

    @DynamoDBAttribute(attributeName = "menu_signature_text")
    private String menuSignatureText;

    @DynamoDBAttribute(attributeName = "primary_location_source")
    @DynamoDBTypeConvertedEnum
    private LocationSource primaryLocationSource;

    @DynamoDBVersionAttribute
    private Long version;

}
