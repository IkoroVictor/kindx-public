package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.inject.Inject;
import io.kindx.entity.MenuFoodItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuFoodItemDao extends BaseDao {

    @Inject
    public MenuFoodItemDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public List<DynamoDBMapper.FailedBatch> saveMenuFoodItems(Iterable<MenuFoodItem> items) {
        return dynamoDbMapper.batchSave(items);
    }

    public List<MenuFoodItem> getUserFoodItems(String userId, String menuId) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":u", new AttributeValue().withS(userId));
        attributeValueMap.put(":m", new AttributeValue().withS(menuId));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#uk", MenuFoodItem.USER_ID_ATTR_NAME);
        attributeNameMap.put("#mk", MenuFoodItem.MENU_ID_ATTR_NAME);

        DynamoDBQueryExpression<MenuFoodItem> queryExpression = new DynamoDBQueryExpression<MenuFoodItem>()
                .withKeyConditionExpression("#uk = :u and #mk = :m")
                .withIndexName(MenuFoodItem.USER_MENU_SYSTEM_NAME_INDEX)
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withConsistentRead(false);
        return dynamoDbMapper.query(MenuFoodItem.class, queryExpression);
    }

    public List<MenuFoodItem> getUserFoodItemsByKitchen(String userId, String kitchenId) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":u", new AttributeValue().withS(userId));
        attributeValueMap.put(":k", new AttributeValue().withS(kitchenId));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#uk", MenuFoodItem.USER_ID_ATTR_NAME);
        attributeNameMap.put("#kk", MenuFoodItem.KITCHEN_ID_ATTR_NAME);

        DynamoDBQueryExpression<MenuFoodItem> queryExpression = new DynamoDBQueryExpression<MenuFoodItem>()
                .withKeyConditionExpression("#uk = :u and #kk = :k")
                .withIndexName(MenuFoodItem.USER_KITCHEN_INDEX)
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withConsistentRead(false);
        return dynamoDbMapper.query(MenuFoodItem.class, queryExpression);
    }
    public List<MenuFoodItem> getFoodItemsForMenu(String menuId) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":m", new AttributeValue().withS(menuId));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#mk", MenuFoodItem.MENU_ID_ATTR_NAME);

        DynamoDBQueryExpression<MenuFoodItem> queryExpression = new DynamoDBQueryExpression<MenuFoodItem>()
                .withKeyConditionExpression("#mk = :m")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withConsistentRead(true);
        return dynamoDbMapper.query(MenuFoodItem.class, queryExpression);
    }
}
