package io.kindx.elasticsearch;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.kindx.constants.Language;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.*;
import java.util.stream.Collectors;


public class ElasticSearchService {

    private static final String ES_MENU_INDEX = "menus-idx";
    private static final String ES_LINES_INDEX = "lines-idx";
    private static final String ES_KITCHEN_INDEX = "kitchen-idx";
    private static final String ES_POLLED_PLACES_INDEX = "polled-places-idx";
    private static final String ES_USAGE_EVENT_INDEX = "usage-events-idx";
    private static final String ES_KITCHEN_CONF_INDEX = "kitchen-conf-idx";
    private static final String ES_FOOD_ITEMS_INDEX = "food-items-idx";
    private static final String ES_USER_LAST_LOCATION = "user-last-location-idx";
    private static final long DEFAULT_SCROLL_TTL_SECONDS = 300;
    private static final String MENU_PIPELINE = "menu-pipeline";

    private static final String ES_LINES_INDEX_ANALYZER = "text_analyzer";

    private static final Logger logger = LogManager.getLogger(ElasticSearchService.class);

    private RestHighLevelClient elasticSearchClient;
    private ObjectMapper objectMapper;

    @Inject
    public ElasticSearchService(RestHighLevelClient elasticSearchClient,
                                ObjectMapper objectMapper) {
        this.elasticSearchClient = elasticSearchClient;
        this.objectMapper = objectMapper;
    }

    public SearchResponse searchMenuIndex(SearchSourceBuilder searchBuilder, boolean isScrollable) {
        return searchIndex(
                new String[]{ES_MENU_INDEX},
                searchBuilder,
                IndicesOptions.lenientExpandOpen(),
                RequestOptions.DEFAULT,
                isScrollable);
    }

    public SearchResponse searchPolledPlacesIndex(SearchSourceBuilder searchBuilder) {
        return searchIndex(
                new String[]{ES_POLLED_PLACES_INDEX},
                searchBuilder,
                IndicesOptions.lenientExpandOpen(),
                RequestOptions.DEFAULT,
                true);
    }

    public SearchResponse searchUserLastLocationIndex(SearchSourceBuilder searchBuilder) {
        return searchIndex(
                new String[]{ES_USER_LAST_LOCATION},
                searchBuilder,
                IndicesOptions.lenientExpandOpen(),
                RequestOptions.DEFAULT,
                true);
    }

    public SearchResponse searchLinesIndex(SearchSourceBuilder searchBuilder, Language... languages) {
        List<String> languageIndices = buildIndexNamesFromLanguages(Arrays.asList(languages), ES_LINES_INDEX);
        return searchIndex(
                languageIndices.toArray(new String[0]),
                searchBuilder,
                IndicesOptions.lenientExpandOpen(),
                RequestOptions.DEFAULT,
                false);
    }

    @SneakyThrows
    public SearchResponse searchScroll(String scrollId, long keepAliveSeconds) {
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(TimeValue.timeValueSeconds(keepAliveSeconds));
        SearchResponse response =  elasticSearchClient.scroll(scrollRequest, RequestOptions.DEFAULT);
        //clearScroll(scrollId);
        return response;
    }


    @SneakyThrows
    public void clearScroll(String... scrollIds) {
        ClearScrollRequest request = new ClearScrollRequest();
        request.setScrollIds(Arrays.asList(scrollIds));
        elasticSearchClient.clearScroll(request, RequestOptions.DEFAULT);
    }

    @SneakyThrows
    public AnalyzeResponse analyzeAsciiFolded(String... texts) {
        AnalyzeRequest request = AnalyzeRequest.buildCustomAnalyzer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("asciifolding")
                .build(texts);
        return elasticSearchClient.indices().analyze(request, RequestOptions.DEFAULT);
    }

    @SneakyThrows
    public AnalyzeResponse analyzeHtmlStrip(String text) {
        AnalyzeRequest request = AnalyzeRequest.buildCustomAnalyzer("keyword")
                .addCharFilter("html_strip")
                .addTokenFilter("trim")
                .build(text);
        return elasticSearchClient.indices().analyze(request, RequestOptions.DEFAULT);
    }

    public AnalyzeResponse analyzeInLineIndex(Language language, String... texts) {
        String index = buildIndexNamesFromLanguages(Collections.singletonList(language), ES_LINES_INDEX).get(0);
        return analyzeInIndex(index, ES_LINES_INDEX_ANALYZER, RequestOptions.DEFAULT, texts);
    }


    @SneakyThrows
    private AnalyzeResponse analyzeInIndex(String index, String analyzer, RequestOptions requestOptions, String... texts) {
        AnalyzeRequest analyzeRequest = AnalyzeRequest.withIndexAnalyzer(index, analyzer, texts);
        return elasticSearchClient.indices().analyze(analyzeRequest, requestOptions);
    }


    public void putInFoodItemsIndex(Map data) {
        putBulkInIndex(data,new String[]{ES_FOOD_ITEMS_INDEX}, null, RequestOptions.DEFAULT, true);
    }

    public void putInUserLastLocation(Object data, String id) {
        putInIndex(data, id, ES_USER_LAST_LOCATION, RequestOptions.DEFAULT);
    }

    public void putInKitchenIndex(Object data, String id) {
        putInIndex(data, id, ES_KITCHEN_INDEX, RequestOptions.DEFAULT);
    }

    public void putInKitchenConfIndex(Object data, String id) {
        putInIndex(data, id, ES_KITCHEN_CONF_INDEX, RequestOptions.DEFAULT);
    }


    public void putInUsageEventsIndex(Object data, String id) {
        putInIndex(data, id, ES_USAGE_EVENT_INDEX, RequestOptions.DEFAULT);
    }

    public void putBulkInUsageEventsIndex(Map data) {
        putBulkInIndex(data,  new String[]{ES_USAGE_EVENT_INDEX}, null,  RequestOptions.DEFAULT, true);
    }
    public void putBulkInPolledPlacesIndex(Map data, boolean replaceExisting) {
        putBulkInIndex(data,  new String[]{ES_POLLED_PLACES_INDEX}, null,  RequestOptions.DEFAULT, replaceExisting);
    }

    public void putInMenuIndex(Object data, String id) {
        putBulkInIndex(
                Collections.singletonMap(id, data),
                new String[]{ES_MENU_INDEX},
                MENU_PIPELINE,
                RequestOptions.DEFAULT, true);
    }

    public void putInMenuIndex(Object data, String id, String pipeline) {
        putBulkInIndex(
                Collections.singletonMap(id, data),
                new String[]{ES_MENU_INDEX},
                pipeline,
                RequestOptions.DEFAULT, true);
    }

    public void putInLinesIndex(Map data, Language... languages) {
        List<String> languageIndices = buildIndexNamesFromLanguages(Arrays.asList(languages), ES_LINES_INDEX);
        putBulkInIndex(data, languageIndices.toArray(new String[0]),null, RequestOptions.DEFAULT, true);
    }

    public GetResponse getLineFromLineIndex(String lineId, Language language) {
        String index = buildIndexNamesFromLanguages(Collections.singletonList(language), ES_LINES_INDEX).get(0);
        return getItemInIndex(lineId, index, RequestOptions.DEFAULT);
    }

    public GetResponse getMenu(String menuId) {
        return getItemInIndex(menuId, ES_MENU_INDEX, RequestOptions.DEFAULT);
    }

    public GetResponse getUserLastLocation(String userId) {
        return getItemInIndex(userId, ES_USER_LAST_LOCATION, RequestOptions.DEFAULT);
    }

    public BulkByScrollResponse deleteMenusByQuery(QueryBuilder builder) {
        return deleteByQueryFromIndices(builder, RequestOptions.DEFAULT, ES_MENU_INDEX);
    }

    public DeleteResponse deleteMenu(String menuId) {
        return deleteFromIndex(menuId,  RequestOptions.DEFAULT, ES_MENU_INDEX);
    }

    public DeleteResponse deleteKitchen(String kitchenId) {
        return deleteFromIndex(kitchenId,  RequestOptions.DEFAULT, ES_KITCHEN_INDEX);
    }

    public DeleteResponse deleteKitchenConf(String kitchenId) {
        return deleteFromIndex(kitchenId,  RequestOptions.DEFAULT, ES_KITCHEN_CONF_INDEX);
    }

    public DeleteResponse deletePolledPlace(String placeId) {
        return deleteFromIndex(placeId,  RequestOptions.DEFAULT, ES_POLLED_PLACES_INDEX);
    }

    public DeleteResponse deleteUserLastLocation(String userId) {
        return deleteFromIndex(userId,  RequestOptions.DEFAULT, ES_USER_LAST_LOCATION);
    }


    public BulkByScrollResponse deleteMenuLinesByQuery(QueryBuilder builder) {
        List<String> languageIndices = buildIndexNamesFromLanguages(Arrays.asList(Language.values()), ES_LINES_INDEX);
        languageIndices.add(ES_LINES_INDEX);
        return deleteByQueryFromIndices(builder, RequestOptions.DEFAULT,  languageIndices.toArray(new String[0]));
    }

    @SneakyThrows
    private GetResponse getItemInIndex(String id, String index, RequestOptions requestOptions) {
        GetRequest getRequest  = new GetRequest(index, id);
        return elasticSearchClient.get(getRequest, requestOptions);
    }

    @SneakyThrows
    private void putBulkInIndex(Map data, String[] indices, String pipeline,
                                RequestOptions requestOptions,
                                boolean replaceExisting) {
        if (!data.isEmpty()) {
            BulkRequest bulkRequest = new BulkRequest().pipeline(pipeline);
            Arrays.asList(indices).forEach(index -> {
                for (Object id : data.keySet()) {
                    String dataString = toJsonString(data.get(id));
                    if (replaceExisting) {
                        UpdateRequest request = new UpdateRequest(index, id.toString());
                        request.doc(dataString, XContentType.JSON);
                        request.upsert(dataString, XContentType.JSON);
                        bulkRequest.add(request);
                    } else {
                        IndexRequest request = Requests.indexRequest(index)
                                .id(id.toString())
                                .opType(DocWriteRequest.OpType.CREATE)
                                .source(dataString, XContentType.JSON);
                        bulkRequest.add(request);
                    }
                }
            });
            elasticSearchClient.bulk(bulkRequest, requestOptions);
        }
    }

    @SneakyThrows
    private void putInIndex(Object data, String id, String index, RequestOptions requestOptions) {
        String dataString = objectMapper.writeValueAsString(data);
        UpdateRequest request = new UpdateRequest(index, id);
        request.doc(dataString, XContentType.JSON);
        request.upsert(dataString, XContentType.JSON);
        elasticSearchClient.update(request, requestOptions);
    }

    @SneakyThrows
    private SearchResponse searchIndex(String[] indices,
                                       SearchSourceBuilder searchSourceBuilder,
                                       IndicesOptions indicesOptions,
                                       RequestOptions requestOptions,
                                       boolean scroll) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(indices);
        searchRequest.indicesOptions(indicesOptions);
        searchRequest.allowPartialSearchResults(true);
        searchRequest.source(searchSourceBuilder);
        if (scroll) {
            searchRequest.scroll(TimeValue.timeValueMinutes(DEFAULT_SCROLL_TTL_SECONDS));
        }
        return elasticSearchClient.search(searchRequest, requestOptions);
    }

    @SneakyThrows
    private BulkResponse deleteFromIndices(String id, RequestOptions requestOptions, String... indices) {
        BulkRequest bulkRequest = new BulkRequest();
        Arrays.asList(indices).forEach(index -> {
            DeleteRequest deleteRequest = new DeleteRequest()
                    .index(index)
                    .id(id);
            bulkRequest.add(deleteRequest);
        });
        return elasticSearchClient.bulk(bulkRequest, requestOptions);
    }

    @SneakyThrows
    private DeleteResponse deleteFromIndex(String id, RequestOptions requestOptions, String index) {
        DeleteRequest deleteRequest = new DeleteRequest()
                .index(index)
                .id(id);
        return elasticSearchClient.delete(deleteRequest, requestOptions);
    }

    @SneakyThrows
    private BulkByScrollResponse deleteByQueryFromIndices(QueryBuilder deleteQuery,
                                                          RequestOptions requestOptions,
                                                          String... indices) {
        DeleteByQueryRequest request = new DeleteByQueryRequest(indices)
                .setAbortOnVersionConflict(false)
                .setQuery(deleteQuery);
        return elasticSearchClient.deleteByQuery(request, requestOptions);
    }

    private List<String> buildIndexNamesFromLanguages(List<Language> languages, String indexPrefix) {
        return languages.stream()
                .map(language -> indexPrefix + "-" + language.getValue())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String toJsonString(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
