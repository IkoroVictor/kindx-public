package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.inject.Inject;
import io.kindx.entity.FoodItemAggregation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FoodItemAggregationDao extends BaseDao {

    @Inject
    public FoodItemAggregationDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public Optional<FoodItemAggregation> findAggregation(String menuId, String systemName) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":m", new AttributeValue().withS(menuId));
        attributeValueMap.put(":s", new AttributeValue().withS(systemName));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#mk", FoodItemAggregation.MENU_ID_ATTR_NAME);
        attributeNameMap.put("#sk", FoodItemAggregation.SYSTEM_NAME_ATTR_NAME);

        DynamoDBQueryExpression<FoodItemAggregation> queryExpression = new DynamoDBQueryExpression<FoodItemAggregation>()
                .withKeyConditionExpression("#mk = :m and #sk = :s")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withConsistentRead(true);
        List<FoodItemAggregation> results = dynamoDbMapper.query(FoodItemAggregation.class, queryExpression);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }


    public List<FoodItemAggregation> findAggregationsForMenu(String menuId) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":m", new AttributeValue().withS(menuId));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#mk", FoodItemAggregation.MENU_ID_ATTR_NAME);

        DynamoDBQueryExpression<FoodItemAggregation> queryExpression = new DynamoDBQueryExpression<FoodItemAggregation>()
                .withKeyConditionExpression("#mk = :m")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withConsistentRead(true);
        return dynamoDbMapper.query(FoodItemAggregation.class, queryExpression);
    }
}
