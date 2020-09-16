package io.kindx.backoffice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.kindx.backoffice.dto.events.JanitorEvent;
import io.kindx.backoffice.dto.places.PolledPlacesRestaurant;
import io.kindx.constants.JanitorEventType;
import io.kindx.dao.KitchenDao;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.Kitchen;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class PlacesToKitchenService {

    private static final String VALIDATED_FIELD_NAME = "validated";
    private static final int SCROLL_PAGE_SIZE = 20;
    private final Validator validator;

    private KitchenDao kitchenDao;
    private ElasticSearchService elasticSearchService;
    private QueueService queueService;
    private ObjectMapper objectMapper;

    @Inject
    public PlacesToKitchenService(KitchenDao kitchenDao,
                                  ElasticSearchService elasticSearchService,
                                  QueueService queueService,
                                  ObjectMapper objectMapper) {
        this.kitchenDao = kitchenDao;
        this.elasticSearchService = elasticSearchService;
        this.queueService = queueService;
        this.objectMapper = objectMapper;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }


    @SneakyThrows
    public Map scheduleValidatedForProcessing() {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery(VALIDATED_FIELD_NAME, true))
                .size(SCROLL_PAGE_SIZE);
        return processSearchResponse(elasticSearchService.searchPolledPlacesIndex(sourceBuilder));
    }

    private Map processSearchResponse (SearchResponse response) {
        long total = 0;
        long processed = 0;
        List<PolledPlacesRestaurant> hits;
        do {
            hits = mapHits(response.getHits());
            total += hits.size();
            processed += processHits(hits);
            response = elasticSearchService.searchScroll(response.getScrollId(), 60);
        } while (!hits.isEmpty() && response.getScrollId() != null);

        Map<String, Object> map = new HashMap<>();
        map.put("total", total);
        map.put("processed", processed);
        return map;
    }

    private int processHits(List<PolledPlacesRestaurant> hits) {
        Map<String, PolledPlacesRestaurant> failed = new HashMap<>();
        List<PolledPlacesRestaurant> toPublish = new ArrayList<>();
        for (PolledPlacesRestaurant hit : hits) {
            Set<String> messages = validationMessages(hit);
            if (!messages.isEmpty()) {
                hit.setValidationMessages(String.join("\n", messages));
                failed.put(hit.getPlacesId(), hit);
            } else {
                //Clear prior validation messages
                hit.setValidationMessages(null);
                toPublish.add(hit);
            }
        }
        elasticSearchService.putBulkInPolledPlacesIndex(failed, true);
        publishMessages(toPublish);
        return toPublish.size();
    }

    private void publishMessages(List<PolledPlacesRestaurant> successful) {
        queueService.enqueuePlacesToKitchenMessages(successful);
        queueService.enqueueJanitorEventMessages(
                successful.stream().map(s -> JanitorEvent.builder()
                        .type(JanitorEventType.POLLED_PLACES)
                        .value(s.getPlacesId()).build())
                        .collect(Collectors.toList())
        );
    }

    @SneakyThrows
    private List<PolledPlacesRestaurant> mapHits(SearchHits hits) {
        if (hits == null || hits.getTotalHits().value == 0 ) {
            return Collections.emptyList();
        }
        List<PolledPlacesRestaurant> list = new ArrayList<>();
        for (SearchHit hit : hits.getHits()) {
            PolledPlacesRestaurant polledPlacesRestaurant = objectMapper.readValue(hit.getSourceAsString(),
                    PolledPlacesRestaurant.class);
            list.add(polledPlacesRestaurant);
        }
        return list;
    }

    private Set<String> validationMessages(PolledPlacesRestaurant restaurant) {
        Set<ConstraintViolation<PolledPlacesRestaurant>> violations =
                validator.validate(restaurant);
        Set<String> validationMessages = violations.
                stream()
                .map(v -> String.format("[%s]: %s", v.getPropertyPath(), v.getMessage()))
                .collect(Collectors.toCollection(HashSet::new));

        if (StringUtils.isNotBlank(restaurant.getFacebookPageId()))  {
            List<Kitchen> kitchensByFacebookId = kitchenDao.getKitchensByFacebookId(restaurant.getFacebookPageId());
            if (!kitchensByFacebookId.isEmpty()) {
                validationMessages.add(String.format("Kitchen with Facebook Id '%s' already exists [%s]",
                        restaurant.getFacebookPageId(), kitchensByFacebookId.get(0).getKitchenId()));
            }
        }
        if ((restaurant.getMenuPageUrls() == null || restaurant.getMenuPageUrls().isEmpty())
                && (restaurant.getPdfUrls() == null || restaurant.getPdfUrls().isEmpty())) {
            validationMessages.add("No Menu or pdf urls specified");
        } else {
            try {
                for (String s : restaurant.getMenuPageUrls()) { new URL(s); }
                for (String s : restaurant.getPdfUrls()) { new URL(s); }
            } catch (MalformedURLException ex) {
                validationMessages.add("Invalid Url: " + ex.getMessage());
            }
        }
        return validationMessages;
    }

}
