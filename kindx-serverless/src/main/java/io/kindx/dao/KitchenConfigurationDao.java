package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.inject.Inject;
import io.kindx.entity.KitchenConfiguration;

import java.util.*;

public class KitchenConfigurationDao extends BaseDao {

    @Inject
    public KitchenConfigurationDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public Optional<KitchenConfiguration> getActiveKitchenConfiguration(String kitchenId) {
        return getKitchenConfiguration(kitchenId, true);
    }

    public Optional<KitchenConfiguration> getKitchenConfiguration(String kitchenId, boolean onlyActive) {
        DynamoDBQueryExpression<KitchenConfiguration> queryExpression =  new DynamoDBQueryExpression<KitchenConfiguration>()
                .withHashKeyValues(KitchenConfiguration.builder().kitchenId(kitchenId).build());
        if (onlyActive) {
            Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
            attributeValueMap.put(":d", new AttributeValue().withN("0"));
            queryExpression.withExpressionAttributeValues(attributeValueMap)
                    .withFilterExpression("is_disabled = :d");
        }
        List<KitchenConfiguration> results = dynamoDbMapper.query(KitchenConfiguration.class, queryExpression);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }


    public List<KitchenConfiguration> getActiveKitchenConfigurations() {
        DynamoDBScanExpression scanExpression =  new DynamoDBScanExpression()
                .withFilterExpression("is_disabled = :d")
                .withExpressionAttributeValues(Collections.singletonMap(":d", new AttributeValue().withN("0")));
        return dynamoDbMapper.scan(KitchenConfiguration.class, scanExpression);
    }
}
