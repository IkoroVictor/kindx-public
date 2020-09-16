package io.kindx.backoffice.service;

import com.google.inject.Inject;
import io.kindx.dao.FoodItemAggregationDao;
import io.kindx.entity.FoodItemAggregation;
import io.kindx.entity.MenuFoodItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FoodItemAggregationService {

    private static final Logger logger = LogManager.getLogger(FoodItemAggregationService.class);

    private FoodItemAggregationDao dao;

    @Inject
    public FoodItemAggregationService(FoodItemAggregationDao dao) {
        this.dao = dao;
    }

    public void aggregate(List<MenuFoodItem> foodItems) {
        //Grouped by menus
        Map<String, List<MenuFoodItem>> menuMenuFoodItemsMap = foodItems.stream()
                .collect(Collectors.groupingBy(MenuFoodItem::getMenuId));

        menuMenuFoodItemsMap.forEach((key, list) -> {
            //Grouped by menu and system name;
            Map<String, List<MenuFoodItem>> itemsMap = list.stream()
                    .collect(Collectors.groupingBy(MenuFoodItem::getSystemName));
            itemsMap.forEach((k, v) -> {
                MenuFoodItem item = v.get(0);
                try {
                    dao.save(() -> mapToAggregation(item, v.size()));
                } catch (Exception ex) {
                    logger.error("Could not save aggregation [menuId: '{}', systemName: '{}', count: {}]",
                            item.getMenuId(),  item.getSystemName(), v.size(), ex);
                }
            });

        });
    }

    private FoodItemAggregation mapToAggregation(MenuFoodItem item, long count) {
        FoodItemAggregation aggregation = dao.findAggregation(item.getMenuId(), item.getSystemName())
                .orElse(FoodItemAggregation.builder()
                        .menuId(item.getMenuId())
                        .name(item.getName())
                        .systemName(item.getSystemName())
                        .count(0L)
                        .build());
        aggregation.setCount(count + aggregation.getCount());
        aggregation.setUpdatedTimestamp(new Date().getTime());
        return aggregation;
    }


}
