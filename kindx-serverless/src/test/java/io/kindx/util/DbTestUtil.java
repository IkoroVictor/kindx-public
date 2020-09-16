package io.kindx.util;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.*;
import io.kindx.entity.*;


public class DbTestUtil {
    private static AmazonDynamoDB DB_INSTANCE;

    public static AmazonDynamoDB getDynamoDb() {
        if (DB_INSTANCE == null) {
            DB_INSTANCE = DynamoDBEmbedded.create().amazonDynamoDB();
        }
        return DB_INSTANCE;
    }


    public static void createAllTables() {
        createMenuTable();
        createMenuGeoHashTable();
        createMenuFoodItemTable();
        createKitchenTable();
        createKitchenConfigTable();
        createMenuConfigTable();
        createUserKitchenTable();
        createUserTable();
        createUserNotificationTable();
        createFoodItemsAggregationTable();
        createLocationTable();
        createPlacesCacheTable();
    }

    public static void dropAllTables() {
        dropTables(
                Kitchen.KITCHEN_TABLE_NAME,
                KitchenConfiguration.TABLE_NAME,
                Menu.TABLE_NAME,
                MenuFoodItem.TABLE_NAME,
                MenuGeoHash.TABLE_NAME,
                User.TABLE_NAME,
                UserKitchenMapping.TABLE_NAME,
                UserNotification.TABLE_NAME,
                FoodItemAggregation.TABLE_NAME,
                MenuConfiguration.TABLE_NAME,
                Location.TABLE_NAME,
                PlacesCacheEntry.TABLE_NAME
        );
    }

    public static void dropTables(String... tables) {
        for (String table: tables) {
            getDynamoDb().deleteTable(table);
        }
    }

    public static void createKitchenTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(Kitchen.KITCHEN_TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(Kitchen.KITCHEN_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(Kitchen.FACEBOOK_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(Kitchen.GOOGLE_PLACES_ID, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(Kitchen.KITCHEN_ID_ATTR_NAME, KeyType.HASH)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndex()
                                .withIndexName(Kitchen.KITCHEN_FACEBOOK_INDEX)
                                .withKeySchema(new KeySchemaElement(Kitchen.FACEBOOK_ID_ATTR_NAME, KeyType.HASH))
                                .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                .withProvisionedThroughput(new ProvisionedThroughput()
                                        .withReadCapacityUnits(25L)
                                        .withWriteCapacityUnits(25L)),
                        new GlobalSecondaryIndex()
                                .withIndexName(Kitchen.KITCHEN_PLACES_INDEX)
                                .withKeySchema(new KeySchemaElement(Kitchen.GOOGLE_PLACES_ID, KeyType.HASH))
                                .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                .withProvisionedThroughput(new ProvisionedThroughput()
                                        .withReadCapacityUnits(25L)
                                        .withWriteCapacityUnits(25L))
                )
        );
    }


    public static void createKitchenConfigTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(KitchenConfiguration.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(KitchenConfiguration.KITCHEN_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(KitchenConfiguration.CREATED_TIMESTAMP_ATTR_NAME, ScalarAttributeType.N)
                )
                .withKeySchema(
                        new KeySchemaElement(KitchenConfiguration.KITCHEN_ID_ATTR_NAME, KeyType.HASH),
                        new KeySchemaElement(KitchenConfiguration.CREATED_TIMESTAMP_ATTR_NAME, KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))

        );
    }

    public static void createMenuConfigTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(MenuConfiguration.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(MenuConfiguration.KITCHEN_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(MenuConfiguration.ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(MenuConfiguration.TYPE_ATTR_NAME, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(MenuConfiguration.KITCHEN_ID_ATTR_NAME, KeyType.HASH),
                        new KeySchemaElement(MenuConfiguration.ID_ATTR_NAME, KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndex()
                                .withIndexName(MenuConfiguration.TYPE_INDEX)
                                .withKeySchema(new KeySchemaElement(MenuConfiguration.TYPE_ATTR_NAME, KeyType.HASH))
                                .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                .withProvisionedThroughput(new ProvisionedThroughput()
                                        .withReadCapacityUnits(25L)
                                        .withWriteCapacityUnits(25L))
                )

        );
    }

    public static void createMenuTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(Menu.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(Menu.KITCHEN_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(Menu.MENU_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(Menu.MENU_CONFIGURATION_ID_ATTR_NAME, ScalarAttributeType.S)
                        )
                .withKeySchema(
                        new KeySchemaElement(Menu.KITCHEN_ID_ATTR_NAME, KeyType.HASH),
                        new KeySchemaElement(Menu.MENU_ID_ATTR_NAME, KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndex()
                                .withIndexName(Menu.MENU_CONFIGURATION_INDEX)
                                .withKeySchema(new KeySchemaElement(Menu.MENU_CONFIGURATION_ID_ATTR_NAME, KeyType.HASH))
                                .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                .withProvisionedThroughput(new ProvisionedThroughput()
                                        .withReadCapacityUnits(25L)
                                        .withWriteCapacityUnits(25L))
                )

        );
    }

    public static void createUserTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(User.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(User.USER_ID_ATTR_NAME, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(User.USER_ID_ATTR_NAME, KeyType.HASH)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))

        );
    }public static void createPlacesCacheTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(PlacesCacheEntry.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(PlacesCacheEntry.PLACES_ID_ATTR_NAME, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(PlacesCacheEntry.PLACES_ID_ATTR_NAME, KeyType.HASH)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))

        );
    }

    public static void createLocationTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(Location.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(Location.LOCATION_ID_ATTR_NAME, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(Location.LOCATION_ID_ATTR_NAME, KeyType.HASH)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))

        );
    }


    public static void createUserKitchenTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(UserKitchenMapping.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(UserKitchenMapping.USER_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(UserKitchenMapping.KITCHEN_ID_ATTR_NAME, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(UserKitchenMapping.KITCHEN_ID_ATTR_NAME, KeyType.HASH),
                        new KeySchemaElement(UserKitchenMapping.USER_ID_ATTR_NAME, KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndex()
                                .withIndexName(UserKitchenMapping.USER_ID_INDEX)
                                .withKeySchema(new KeySchemaElement(UserKitchenMapping.USER_ID_ATTR_NAME, KeyType.HASH))
                                .withProjection(new Projection()
                                        .withProjectionType(ProjectionType.INCLUDE)
                                        .withNonKeyAttributes(UserKitchenMapping.FOOD_PREFERENCES_ATTR_NAME))
                                .withProvisionedThroughput(new ProvisionedThroughput()
                                        .withReadCapacityUnits(25L)
                                        .withWriteCapacityUnits(25L))
                )
        );
    }

    public static void createUserNotificationTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(UserNotification.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(UserNotification.USER_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(UserNotification.KITCHEN_MENU_ID_ATTR_NAME, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(UserNotification.USER_ID_ATTR_NAME, KeyType.HASH),
                        new KeySchemaElement(UserNotification.KITCHEN_MENU_ID_ATTR_NAME, KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))

        );
    }

    public static void createFoodItemsAggregationTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(FoodItemAggregation.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(FoodItemAggregation.SYSTEM_NAME_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(FoodItemAggregation.MENU_ID_ATTR_NAME, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(FoodItemAggregation.MENU_ID_ATTR_NAME, KeyType.HASH),
                        new KeySchemaElement(FoodItemAggregation.SYSTEM_NAME_ATTR_NAME, KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))
        );
    }


    public static void createMenuFoodItemTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(MenuFoodItem.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(MenuFoodItem.MENU_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(MenuFoodItem.USER_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(MenuFoodItem.KITCHEN_ID_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(MenuFoodItem.USER_ID_NAME_ATTR_NAME, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(MenuFoodItem.MENU_ID_ATTR_NAME, KeyType.HASH),
                        new KeySchemaElement(MenuFoodItem.USER_ID_NAME_ATTR_NAME, KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndex()
                                .withIndexName(MenuFoodItem.USER_MENU_SYSTEM_NAME_INDEX)
                                .withKeySchema(
                                        new KeySchemaElement(MenuFoodItem.USER_ID_ATTR_NAME, KeyType.HASH),
                                        new KeySchemaElement(MenuFoodItem.MENU_ID_ATTR_NAME, KeyType.RANGE))
                                .withProjection(new Projection()
                                        .withProjectionType(ProjectionType.INCLUDE)
                                        .withNonKeyAttributes(MenuFoodItem.SYSTEM_NAME_ATTR_NAME)
                                )
                                .withProvisionedThroughput(new ProvisionedThroughput()
                                        .withReadCapacityUnits(25L)
                                        .withWriteCapacityUnits(25L)),
                        new GlobalSecondaryIndex()
                                .withIndexName(MenuFoodItem.USER_KITCHEN_INDEX)
                                .withKeySchema(
                                        new KeySchemaElement(MenuFoodItem.USER_ID_ATTR_NAME, KeyType.HASH),
                                        new KeySchemaElement(MenuFoodItem.KITCHEN_ID_ATTR_NAME, KeyType.RANGE))
                                .withProjection(new Projection()
                                        .withProjectionType(ProjectionType.INCLUDE)
                                        .withNonKeyAttributes(MenuFoodItem.USER_ID_NAME_ATTR_NAME)
                                )
                                .withProvisionedThroughput(new ProvisionedThroughput()
                                        .withReadCapacityUnits(25L)
                                        .withWriteCapacityUnits(25L))
                )
        );
    }

    public static void createMenuGeoHashTable() {
        getDynamoDb().createTable(new CreateTableRequest()
                .withTableName(MenuGeoHash.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition(MenuGeoHash.HASH_KEY_ATTR_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(MenuGeoHash.MENU_ID_ATTR_NAME, ScalarAttributeType.S)
                )
                .withKeySchema(
                        new KeySchemaElement(MenuGeoHash.HASH_KEY_ATTR_NAME, KeyType.HASH),
                        new KeySchemaElement(MenuGeoHash.MENU_ID_ATTR_NAME, KeyType.RANGE)
                )
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(25L)
                        .withWriteCapacityUnits(25L))
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndex()
                                .withIndexName(MenuGeoHash.MENU_ID_INDEX)
                                .withKeySchema(new KeySchemaElement(MenuGeoHash.MENU_ID_ATTR_NAME, KeyType.HASH))
                                .withProjection(new Projection()
                                        .withProjectionType(ProjectionType.KEYS_ONLY)
                                )
                                .withProvisionedThroughput(new ProvisionedThroughput()
                                        .withReadCapacityUnits(25L)
                                        .withWriteCapacityUnits(25L))
                )
        );
    }

}
