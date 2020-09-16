package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.TransactionWriteRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.inject.Inject;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.entity.MenuConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public class MenuConfigurationDao extends BaseDao {

    @Inject
    public MenuConfigurationDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public List<MenuConfiguration> getMenuConfigurationsForKitchen(String kitchenId) {
        DynamoDBQueryExpression<MenuConfiguration> queryExpression = new DynamoDBQueryExpression<MenuConfiguration>()
                .withHashKeyValues(MenuConfiguration.builder().kitchenId(kitchenId).build())
                .withConsistentRead(true);
        return dynamoDbMapper.query(MenuConfiguration.class, queryExpression);
    }

    public List<MenuConfiguration> getMenuConfigurationsByType(MenuConfigurationType type){
        Map<String, AttributeValue> attributeValueMap  = Collections.singletonMap(":t",
                new AttributeValue().withS(type.name()));
        Map<String, String> attributeNameMap  =  Collections.singletonMap("#t", MenuConfiguration.TYPE_ATTR_NAME);

        DynamoDBQueryExpression<MenuConfiguration> queryExpression =  new DynamoDBQueryExpression<MenuConfiguration>()
                .withKeyConditionExpression("#t = :t")
                .withExpressionAttributeNames(attributeNameMap)
                .withExpressionAttributeValues(attributeValueMap)
                .withConsistentRead(false)
                .withIndexName(MenuConfiguration.TYPE_INDEX);
        return dynamoDbMapper.query(MenuConfiguration.class, queryExpression);
    }

    public void updateMenuConfigurations(List<MenuConfiguration> toSave,  List<MenuConfiguration> toDelete) {
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        for (MenuConfiguration m : toSave) {
            transactionWriteRequest.addPut(m);
        }
        for (MenuConfiguration m : toDelete) {
            transactionWriteRequest.addDelete(m);
        }
        executeTransactionWrite(() -> transactionWriteRequest);
    }
}
