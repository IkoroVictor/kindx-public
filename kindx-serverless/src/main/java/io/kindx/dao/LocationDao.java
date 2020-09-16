package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.inject.Inject;
import io.kindx.entity.Location;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LocationDao extends BaseDao {

    @Inject
    public LocationDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }


    public List<Location> getActiveLocations() {
        DynamoDBScanExpression scanExpression =  new DynamoDBScanExpression()
                .withFilterExpression("is_disabled = :d")
                .withExpressionAttributeValues(Collections.singletonMap(":d", new AttributeValue().withN("0")));
        return dynamoDbMapper.scan(Location.class, scanExpression);
    }

    public Optional<Location> getLocation(String locationId){
        DynamoDBQueryExpression<Location> queryExpression =  new DynamoDBQueryExpression<Location>()
                .withHashKeyValues(Location.builder().locationId(locationId).build());
        List<Location> results = dynamoDbMapper.query(Location.class, queryExpression);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

}
