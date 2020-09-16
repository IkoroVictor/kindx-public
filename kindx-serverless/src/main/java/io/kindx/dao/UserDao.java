package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.google.inject.Inject;
import io.kindx.entity.User;

import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDao {
    @Inject
    public UserDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public Optional<User> getUser(String userId){
        DynamoDBQueryExpression<User> queryExpression =  new DynamoDBQueryExpression<User>()
                .withHashKeyValues(User.builder().userId(userId).build());
        List<User> results = dynamoDbMapper.query(User.class, queryExpression);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

}
