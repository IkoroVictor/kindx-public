package io.kindx.backoffice.service;

import com.google.inject.Inject;
import io.kindx.backoffice.exception.MenuDeletionException;
import io.kindx.backoffice.processor.menu.es.ESMenuLineProcessor;
import io.kindx.dao.FoodItemAggregationDao;
import io.kindx.dao.MenuDao;
import io.kindx.dao.MenuFoodItemDao;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.Menu;
import io.kindx.util.ResilienceUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.rest.RestStatus;

import java.util.stream.Collectors;

public class MenuService {

    private ElasticSearchService elasticSearchService;
    private MenuDao menuDao;
    private FoodItemAggregationDao aggregationDao;
    private MenuFoodItemDao foodItemDao;

    private static final Logger logger = LogManager.getLogger(MenuService.class);


    @Inject
    public MenuService(ElasticSearchService elasticSearchService,
                       FoodItemAggregationDao aggregationDao,
                       MenuDao menuDao,
                       MenuFoodItemDao foodItemDao) {
        this.elasticSearchService = elasticSearchService;
        this.aggregationDao = aggregationDao;
        this.menuDao = menuDao;
        this.foodItemDao = foodItemDao;

    }

    public void deleteMenusViaConfigId(String menuConfigurationId) {
        menuDao.getMenusForConfigId(menuConfigurationId)
                .forEach(this::deleteMenu);
    }

    public void deleteMenusViaKitchenId(String kitchenId) {
        menuDao.getMenusForKitchen(kitchenId)
                .forEach(this::deleteMenu);
    }

    public void deleteMenuViaMenuId(String kitchenId, String menuId) {
        menuDao.getMenu(kitchenId, menuId).ifPresent(this::deleteMenu);
    }

    private void deleteMenu(Menu menu) {
        deleteESMenu(menu);
        logger.info("Deleting food item aggregations for menu {}", menu.getMenuId());
        aggregationDao.findAggregationsForMenu(menu.getMenuId())
                .forEach(aggregationDao::delete);
        logger.info("Deleting food items for menu {}", menu.getMenuId());
        foodItemDao.getFoodItemsForMenu(menu.getMenuId())
                .forEach(foodItemDao::delete);
        logger.info("Deleting menu {}", menu.getMenuId());
        menuDao.delete(menu);
    }

    private void deleteESMenu(Menu menu) {
        ResilienceUtil.retryOnException(() -> deleteESMenuData(menu));
    }

    private void deleteESMenuData(Menu menu) {
        //Clean up ES pre-processing "lines" documents
        BulkByScrollResponse response = elasticSearchService.deleteMenuLinesByQuery(QueryBuilders.matchQuery(
                ESMenuLineProcessor.ES_INDEX_TEXT_ID_FIELD,
                menu.getMenuId()));
        long deleteDiff = response.getStatus().getTotal() - response.getStatus().getDeleted();
        if (deleteDiff != 0 ) {
            String message = bulkFailureMessages(response);
            throw new MenuDeletionException(
                    String.format("Could not delete all menu lines for menu with id '%s' in elasticsearch. %d failed. \n %s",
                            menu.getMenuId(), deleteDiff, message ), null);
        }

        //Clean up ES menu document
        DeleteResponse deleteResponse = elasticSearchService.deleteMenu(menu.getMenuId());
        if (deleteResponse.status() != RestStatus.NO_CONTENT
                && deleteResponse.status() != RestStatus.OK ) {
            if (deleteResponse.status() == RestStatus.NOT_FOUND) {
                logger.warn("Menu with id {} not found.....skipping", menu.getMenuId());
            } else throw new MenuDeletionException(
                    String.format("Could not menu '%s' in elasticsearch. Status code %d \n",
                            menu.getMenuId(), deleteResponse.status().getStatus()), null);
        }
    }

    private String bulkFailureMessages(BulkByScrollResponse response) {
        return response.getBulkFailures().stream()
                .map(BulkItemResponse.Failure::getMessage).collect(Collectors.joining("\n"));
    }
}
