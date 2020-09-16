package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.google.inject.Inject;
import io.kindx.entity.PlacesCacheEntry;

import java.util.List;
import java.util.Optional;

public class PlacesCacheDao extends BaseDao {

    @Inject
    public PlacesCacheDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public Optional<PlacesCacheEntry> getEntry(String placesId){
        DynamoDBQueryExpression<PlacesCacheEntry> queryExpression =  new DynamoDBQueryExpression<PlacesCacheEntry>()
                .withHashKeyValues(PlacesCacheEntry.builder().placesId(placesId).build());
        List<PlacesCacheEntry> results = dynamoDbMapper.query(PlacesCacheEntry.class, queryExpression);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

}
