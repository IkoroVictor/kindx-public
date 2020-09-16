package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.inject.Inject;
import io.kindx.entity.Menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MenuDao extends BaseDao {

    @Inject
    public MenuDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public boolean menuExists(String kitchenId, String menuId) {
        Condition kitchenCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(kitchenId));
        Condition menuCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(menuId));

        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(Menu.KITCHEN_ID_ATTR_NAME, kitchenCondition);
        keyConditions.put(Menu.MENU_ID_ATTR_NAME, menuCondition);

        QueryRequest request = new QueryRequest(Menu.TABLE_NAME);
        request.setSelect(Select.COUNT);
        request.setConsistentRead(true);
        request.setKeyConditions(keyConditions);

        QueryResult result = amazonDynamoDB.query(request);
        Integer count = result.getCount();
        return count != null && count > 0;
    }

    public List<Menu> getMenusForKitchen(String kitchenId) {
        DynamoDBQueryExpression<Menu> queryExpression = new DynamoDBQueryExpression<Menu>()
                .withHashKeyValues(Menu.builder().kitchenId(kitchenId).build())
                .withConsistentRead(true);
        return dynamoDbMapper.query(Menu.class, queryExpression);
    }

    public List<Menu> getMenusForConfigId(String configId) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":mc", new AttributeValue().withS(configId));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#mc", Menu.MENU_CONFIGURATION_ID_ATTR_NAME);

        DynamoDBQueryExpression<Menu> queryExpression = new DynamoDBQueryExpression<Menu>()
                .withKeyConditionExpression("#mc = :mc")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withIndexName(Menu.MENU_CONFIGURATION_INDEX)
                .withConsistentRead(false);
        return dynamoDbMapper.query(Menu.class, queryExpression);
    }


    public Optional<Menu> getMenu(String kitchenId, String menuId) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":k", new AttributeValue().withS(kitchenId));
        attributeValueMap.put(":m", new AttributeValue().withS(menuId));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#k", Menu.KITCHEN_ID_ATTR_NAME);
        attributeNameMap.put("#m", Menu.MENU_ID_ATTR_NAME);

        DynamoDBQueryExpression<Menu> queryExpression = new DynamoDBQueryExpression<Menu>()
                .withKeyConditionExpression("#k = :k and #m = :m")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withConsistentRead(true);
        List<Menu> results = dynamoDbMapper.query(Menu.class, queryExpression);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
