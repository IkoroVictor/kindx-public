package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.TransactionWriteRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.inject.Inject;
import io.kindx.entity.Kitchen;
import io.kindx.entity.KitchenConfiguration;
import io.kindx.entity.MenuConfiguration;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

public class KitchenDao extends BaseDao {

    @Inject
    public KitchenDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public List<Kitchen> getActiveKitchens() {
        DynamoDBScanExpression scanExpression =  new DynamoDBScanExpression()
                .withFilterExpression("is_disabled = :d")
                .withExpressionAttributeValues(Collections.singletonMap(":d", new AttributeValue().withN("0")));
        return dynamoDbMapper.scan(Kitchen.class, scanExpression);
    }

    public Optional<Kitchen> getKitchenByKitchenId(String kitchenId) {
        DynamoDBQueryExpression<Kitchen> queryExpression =  new DynamoDBQueryExpression<Kitchen>()
                .withHashKeyValues(Kitchen.builder().kitchenId(kitchenId).build())
                .withConsistentRead(true);
        List<Kitchen> results = dynamoDbMapper.query(Kitchen.class, queryExpression);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Kitchen> getActiveKitchenByKitchenId(String kitchenId) {
        Optional<Kitchen> kitchen = getKitchenByKitchenId(kitchenId);
        return kitchen.isPresent() && !TRUE.equals(kitchen.get().getIsDisabled())
                ? kitchen : Optional.empty();
    }

    public List<Kitchen> getKitchensByPlacesId(String googlePlacesId) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":gp", new AttributeValue().withS(googlePlacesId));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#gp", Kitchen.GOOGLE_PLACES_ID);

        DynamoDBQueryExpression<Kitchen> queryExpression =  new DynamoDBQueryExpression<Kitchen>()
                .withKeyConditionExpression("#gp = :gp")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withIndexName(Kitchen.KITCHEN_PLACES_INDEX)
                .withConsistentRead(false);
        return dynamoDbMapper.query(Kitchen.class, queryExpression);
    }

    public List<Kitchen> getActiveKitchensByPlacesId(String googlePlacesId) {
        List<Kitchen> kitchens = getKitchensByPlacesId(googlePlacesId);
        return kitchens.stream()
                .filter(k -> !TRUE.equals(k.getIsDisabled()))
                .collect(Collectors.toList());
    }

    public List<Kitchen> getKitchensByFacebookId(String facebookId) {
        Map<String, AttributeValue> attributeValueMap  = new HashMap<>();
        attributeValueMap.put(":fb", new AttributeValue().withS(facebookId));

        Map<String, String> attributeNameMap  = new HashMap<>();
        attributeNameMap.put("#fb", Kitchen.FACEBOOK_ID_ATTR_NAME);

        DynamoDBQueryExpression<Kitchen> queryExpression =  new DynamoDBQueryExpression<Kitchen>()
                .withKeyConditionExpression("#fb = :fb")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withIndexName(Kitchen.KITCHEN_FACEBOOK_INDEX)
                .withConsistentRead(false);
        return dynamoDbMapper.query(Kitchen.class, queryExpression);
    }

    public List<Kitchen> getActiveKitchensByFacebookId(String facebookId) {
        List<Kitchen> kitchens = getKitchensByFacebookId(facebookId);
        return kitchens.stream()
                .filter(k -> !TRUE.equals(k.getIsDisabled()))
                .collect(Collectors.toList());
    }

    public void saveKitchenWithConfigurations(Kitchen kitchen,
                                              KitchenConfiguration configuration,
                                              List<MenuConfiguration> toSave,
                                              List<MenuConfiguration> toDelete) {
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(kitchen);
        transactionWriteRequest.addPut(configuration);
        if (toSave != null) {
            for (MenuConfiguration m : toSave) {
                transactionWriteRequest.addPut(m);
            }
        }
        if (toDelete != null) {
            for (MenuConfiguration m : toDelete) {
                transactionWriteRequest.addDelete(m);
            }
        }
        executeTransactionWrite(() -> transactionWriteRequest);
    }


}
