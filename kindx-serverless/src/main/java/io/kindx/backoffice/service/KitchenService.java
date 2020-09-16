package io.kindx.backoffice.service;

import com.google.inject.Inject;
import io.kindx.backoffice.exception.KitchenDeletionException;
import io.kindx.dao.KitchenConfigurationDao;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.UserKitchenMappingDao;
import io.kindx.elasticsearch.ElasticSearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.rest.RestStatus;

public class KitchenService {

    private ElasticSearchService elasticSearchService;
    private MenuService menuService;
    private KitchenDao kitchenDao;
    private UserKitchenMappingDao userKitchenMappingDao;
    private KitchenConfigurationDao configurationDao;

    private static final Logger logger = LogManager.getLogger(KitchenService.class);

    @Inject
    public KitchenService(ElasticSearchService elasticSearchService,
                          MenuService menuService,
                          KitchenDao kitchenDao,
                          UserKitchenMappingDao userKitchenMappingDao,
                          KitchenConfigurationDao configurationDao) {
        this.elasticSearchService = elasticSearchService;
        this.menuService = menuService;
        this.kitchenDao = kitchenDao;
        this.userKitchenMappingDao = userKitchenMappingDao;
        this.configurationDao = configurationDao;
    }

    public void cleanupKitchenById(String kitchenId) {
        deleteKitchen(kitchenId);
    }

    private void deleteKitchen(String kitchenId) {

        logger.info("Deleting menus for kitchen {}", kitchenId);
        menuService.deleteMenusViaKitchenId(kitchenId);

        logger.info("Deleting elasticsearch kitchen data for {}", kitchenId);
        deleteESKitchen(kitchenId);

        logger.info("Deleting user kitchen mappings data for kitchen  {}", kitchenId);
        userKitchenMappingDao.getMappingsForKitchen(kitchenId).forEach(userKitchenMappingDao::delete);

        logger.info("Deleting Kitchen configuration for {}", kitchenId);
        configurationDao.getKitchenConfiguration(kitchenId, false)
                .ifPresent(configurationDao::delete);

        logger.info("Deleting Kitchen {}", kitchenId);
        kitchenDao.getKitchenByKitchenId(kitchenId)
                .ifPresent(kitchenDao::delete);

    }


    private void deleteESKitchen(String kitchenId) {

        //Clean up ES Kitchen configuration document
        DeleteResponse deleteResponse = elasticSearchService.deleteKitchenConf(kitchenId);
        if (deleteResponse.status() != RestStatus.NO_CONTENT
                && deleteResponse.status() != RestStatus.OK ) {
            if (deleteResponse.status() == RestStatus.NOT_FOUND) {
                logger.warn("Kitchen configuration for {} not found in ES.....skipping", kitchenId);
            } else throw new KitchenDeletionException(
                    String.format("Could not delete configuration for kitchen '%s' in elasticsearch. Status code %d \n",
                            kitchenId, deleteResponse.status().getStatus()));
        }

        //Clean up ES Kitchen document
        deleteResponse = elasticSearchService.deleteKitchen(kitchenId);
        if (deleteResponse.status() != RestStatus.NO_CONTENT
                && deleteResponse.status() != RestStatus.OK ) {
            if (deleteResponse.status() == RestStatus.NOT_FOUND) {
                logger.warn("Kitchen record for {} not found in ES.....skipping", kitchenId);
            } else throw new KitchenDeletionException(
                    String.format("Could not kitchen '%s' in elasticsearch. Status code %d \n",
                            kitchenId, deleteResponse.status().getStatus()));
        }
    }

}
