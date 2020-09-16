package io.kindx.backoffice.handler.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import io.kindx.backoffice.service.FoodItemAggregationService;
import io.kindx.entity.MenuFoodItem;
import io.kindx.factory.InjectorFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FoodItemAggregationHandler implements RequestHandler<DynamodbEvent, Integer> {

    private static final Logger logger = LogManager.getLogger(FoodItemAggregationHandler.class);
    private static final String LOG_FORMAT = "[DBStreamRecordEventId: {}] [Message: {}]";

    private FoodItemAggregationService service;
    private DynamoDBMapper mapper;

    public FoodItemAggregationHandler() {
        this.service = InjectorFactory.getInjector().getInstance(FoodItemAggregationService.class);
        mapper = new DynamoDBMapper(null);
    }

    @Override
    public Integer handleRequest(DynamodbEvent input, Context context) {
        try {
            List<MenuFoodItem> items = input.getRecords()
                    .stream()
                    .filter(record -> "INSERT".equals(record.getEventName())) //Aggregate only new items
                    .map(Record::getDynamodb)
                    .map(StreamRecord::getNewImage)
                    .map(this::mapToFoodItem)
                    .collect(Collectors.toList());
            service.aggregate(items);
            logger.info("Successfully aggregated {} items.", items.size());
        } catch (Exception ex) {
            logger.error("Error aggregating food items. {}", ex.getMessage(), ex);
            throw new RuntimeException("Could not aggregate food items", ex);
        }
        return 0;
    }


    private MenuFoodItem mapToFoodItem(Map<String, AttributeValue> valueMap) {
        return mapper.marshallIntoObject(MenuFoodItem.class, valueMap);
    }
}
