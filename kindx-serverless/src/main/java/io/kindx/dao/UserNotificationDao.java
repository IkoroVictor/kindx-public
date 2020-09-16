package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.inject.Inject;
import io.kindx.entity.UserNotification;

import java.util.HashMap;
import java.util.Map;

public class UserNotificationDao extends BaseDao {

    @Inject
    public UserNotificationDao(AmazonDynamoDB amazonDynamoDB) {
        super(amazonDynamoDB);
    }

    public boolean notificationExists(String kitchenMenuId, String userId) {
        Condition kitchenMenuCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(kitchenMenuId));
        Condition userCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(userId));

        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(UserNotification.KITCHEN_MENU_ID_ATTR_NAME, kitchenMenuCondition);
        keyConditions.put(UserNotification.USER_ID_ATTR_NAME, userCondition);

        QueryRequest request = new QueryRequest(UserNotification.TABLE_NAME);
        //TODO: CREATE GSI INDEX
        //request.setIndexName("kitchen-menu-user-index");
        request.setSelect(Select.COUNT);
        request.setKeyConditions(keyConditions);

        QueryResult result = amazonDynamoDB.query(request);
        Integer count = result.getCount();

        return count != null && count > 0;
    }

    public void saveNotification(UserNotification notification) {
        dynamoDbMapper.save(notification);
    }
}
