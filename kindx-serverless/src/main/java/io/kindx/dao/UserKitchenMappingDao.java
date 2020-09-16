package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.inject.Inject;
import io.kindx.entity.UserKitchenMapping;

import java.util.*;

public class UserKitchenMappingDao extends BaseDao {

    @Inject
    public UserKitchenMappingDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public List<UserKitchenMapping> getMappingsForKitchen(String kitchenId){
       DynamoDBQueryExpression<UserKitchenMapping> queryExpression =  new DynamoDBQueryExpression<UserKitchenMapping>()
               .withHashKeyValues(UserKitchenMapping.builder().kitchenId(kitchenId).build())
               .withConsistentRead(true);
       return dynamoDbMapper.query(UserKitchenMapping.class, queryExpression);
    }

    public List<UserKitchenMapping> getUserKitchenMappings(String userId){
        Map<String, AttributeValue> attributeValueMap  = Collections.singletonMap(":u", new AttributeValue().withS(userId));
        Map<String, String> attributeNameMap  =  Collections.singletonMap("#uk", UserKitchenMapping.USER_ID_ATTR_NAME);

        DynamoDBQueryExpression<UserKitchenMapping> queryExpression =  new DynamoDBQueryExpression<UserKitchenMapping>()
                .withKeyConditionExpression("#uk = :u")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withConsistentRead(false)
                .withIndexName(UserKitchenMapping.USER_ID_INDEX);
        return dynamoDbMapper.query(UserKitchenMapping.class, queryExpression);
    }


    public Optional<UserKitchenMapping> getUserKitchenMapping(String userId, String kitchenId) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":u", new AttributeValue().withS(userId));
        attributeValueMap.put(":k", new AttributeValue().withS(kitchenId));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#uk", UserKitchenMapping.USER_ID_ATTR_NAME);
        attributeNameMap.put("#kk", UserKitchenMapping.KITCHEN_ID_ATTR_NAME);

        DynamoDBQueryExpression<UserKitchenMapping> queryExpression = new DynamoDBQueryExpression<UserKitchenMapping>()
                .withKeyConditionExpression("#uk = :u and #kk = :k")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withConsistentRead(true);
        List<UserKitchenMapping> results = dynamoDbMapper.query(UserKitchenMapping.class, queryExpression);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }


}
