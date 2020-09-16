package io.kindx.util.script;

import com.google.inject.Injector;
import io.kindx.backoffice.service.KitchenService;
import io.kindx.dao.KitchenConfigurationDao;
import io.kindx.dao.KitchenDao;
import io.kindx.dao.MenuConfigurationDao;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.Kitchen;
import io.kindx.factory.InjectorFactory;
import io.kindx.util.ResilienceUtil;
import io.kindx.util.TestUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Ignore
public class KitchenScannerScript {
    private static final Logger logger = LogManager.getLogger(KitchenScannerScript.class);


    @ClassRule
    public static final EnvironmentVariables envVariables = TestUtil.loadTestEnvVariables("env/script.env");

    protected Injector injector;
    protected KitchenDao dao;
    protected KitchenConfigurationDao confDao;
    protected MenuConfigurationDao menuConfDao;
    protected KitchenService service;
    protected ElasticSearchService elasticSearchService;

    @Before
    public void setup() {
        injector = InjectorFactory.getInjector();
        dao = InjectorFactory.getInjector().getInstance(KitchenDao.class);
        confDao = InjectorFactory.getInjector().getInstance(KitchenConfigurationDao.class);
        menuConfDao = InjectorFactory.getInjector().getInstance(MenuConfigurationDao.class);
        elasticSearchService = InjectorFactory.getInjector().getInstance(ElasticSearchService.class);
        service = InjectorFactory.getInjector().getInstance(KitchenService.class);
    }


    //@Test
    public void cleanKitchenDuplicate() {
        Map<String, List<String>> ids = new HashMap<>();
        Set<String> pset = dao.getActiveKitchens()
                .stream().filter(k -> k.getGooglePlacesId() != null)
                .map(Kitchen::getGooglePlacesId)
                .collect(Collectors.toSet());

        for (String p : pset) {
            List<String> l = dao.getKitchensByPlacesId(p)
                    .stream()
                    .map(Kitchen::getKitchenId)
                    .collect(Collectors.toList());
            if (l.size() > 1) {
                logger.info("Duplicate kitchens for place id '{}' : [{}] ",
                        p, String.join(",", l));
                ids.put(p, l);
                try {
                    cleanupKitchen(p, l.subList(1, l.size() - 1));
                } catch (Exception ex) {
                    logger.warn("Error cleaning duplicate kitchens for place id {}......skipping... {}",
                            p, ex.getMessage());
                }

            }
        }

        logger.info("Total duplicates = {}....cleaning up kitchens", ids.size());

    }



    private void cleanupKitchen(String placeId, List<String> kIds) {
        logger.info("{} Kitchens found for placeId {}.... Deleting {} kitchens ",
                kIds.size(), placeId, kIds.size() - 1);
        for (String id : kIds) {
            ResilienceUtil.retryOnExceptionSilently(() -> {
                service.cleanupKitchenById(id);
                menuConfDao.getMenuConfigurationsForKitchen(id).forEach(menuConfDao::delete);
                confDao.getKitchenConfiguration(id, false).ifPresent(confDao::delete);
                dao.delete(Kitchen.builder().kitchenId(id).build());
            });

            logger.info("Kitchen {} for place id {} deleted...... ",
                    id,  placeId);
        }
    }

    //@Test
    public void reIndexKitchens() {
        dao.getActiveKitchens().forEach(k -> {
            elasticSearchService.putInKitchenIndex(k, k.getKitchenId());
            logger.info("Kitchen {} indexed...... ", k.getKitchenId());
            confDao.getKitchenConfiguration(k.getKitchenId(), true)
                    .ifPresent(c -> {
                        elasticSearchService.putInKitchenConfIndex(c, c.getKitchenId());
                        logger.info("Kitchen configuration for {} indexed...... ", c.getKitchenId());
                    });

        });
    }

}
