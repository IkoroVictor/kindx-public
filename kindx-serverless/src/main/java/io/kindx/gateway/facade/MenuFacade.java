package io.kindx.gateway.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.kindx.constants.Defaults;
import io.kindx.constants.Language;
import io.kindx.dao.UserKitchenMappingDao;
import io.kindx.dto.GeoPointDto;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.entity.Menu;
import io.kindx.entity.UserKitchenMapping;
import io.kindx.exception.NotFoundException;
import io.kindx.gateway.dto.*;
import io.kindx.gateway.util.DateUtil;
import io.kindx.mapper.MenuMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;

import javax.inject.Named;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MenuFacade {

    private static final String ES_INDEX_FIELD_MENU_DATE = "menuDate";
    private static final String ES_INDEX_FIELD_MENU_LOCATION = "location.geoPoint";
    private static final String ES_INDEX_FIELD_MENU_TEXT = "menuText";
    private static final String ES_INDEX_FIELD_MENU_ID = "menuId";
    private static final String ES_INDEX_FIELD_KITCHEN_ID = "kitchenId";
    private static final String ES_INDEX_FIELD_BUSINESS_NAME = "businessProfile.businessName";


    private ElasticSearchService elasticSearchService;
    private UserKitchenMappingDao userKitchenMappingDao;
    private ObjectMapper objectMapper;
    private MenuMapper menuMapper;
    private double queryMaxGeoDistanceInKm;
    private long keepPageTokenAlive;

    @Inject
    public MenuFacade(ElasticSearchService elasticSearchService,
                      UserKitchenMappingDao userKitchenMappingDao,
                      ObjectMapper objectMapper,
                      MenuMapper menuMapper,
                      @Named("queryMaxGeoDistanceInKm") long queryMaxGeoDistanceInKm,
                      @Named("keepPageTokenAlive") long keepPageTokenAlive) {
        this.elasticSearchService = elasticSearchService;
        this.userKitchenMappingDao = userKitchenMappingDao;
        this.objectMapper = objectMapper;
        this.menuMapper = menuMapper;
        this.queryMaxGeoDistanceInKm = (double) queryMaxGeoDistanceInKm;
        this.keepPageTokenAlive = keepPageTokenAlive;
    }

    public PaginatedContentDto<MenuDto> getMenus(MenuQueryDto query, String userId) {
        String pageToken = query.getPageToken();
        SearchResponse searchResponse;
        if (StringUtils.isNotBlank(pageToken)) {
            searchResponse = elasticSearchService.searchScroll(query.getPageToken(), keepPageTokenAlive);
        } else {
            searchResponse = getMenusFromES(query);
        }
        return mapResponseToDto(searchResponse, null, pageToken, userId);
    }

    @SneakyThrows
    public MenuDto getMenu(String menuId, String userId) {
        GetResponse response = elasticSearchService.getMenu(menuId);
        if (!response.isExists()) {
            throw new NotFoundException("Menu not found");
        }
        Menu menu = objectMapper.readValue(response.getSourceAsString(), Menu.class);
        return menuMapper.toMenuDto(menu, null, null, userId);
    }


    public PaginatedContentDto<MenuDto> search(MenuSearchDto search) {
        String pageToken = search.getPageToken();
        SearchResponse searchResponse;
        if (StringUtils.isNotBlank(pageToken)) {
            searchResponse = elasticSearchService.searchScroll(search.getPageToken(), keepPageTokenAlive);
            //TODO: clean up expired scroll ids
        } else {
            searchResponse = searchAsNew(search);
        }
        return mapResponseToDto(searchResponse, search.getGeoPoint(), pageToken, null);
    }

    public PaginatedContentDto<MenuDto> getTodayMenus(MenuTodayQueryDto todayQuery, String userId) {
        String pageToken = todayQuery.getPageToken();
        SearchResponse searchResponse;
        if (StringUtils.isNotBlank(pageToken)) {
            searchResponse = elasticSearchService.searchScroll(todayQuery.getPageToken(), keepPageTokenAlive);
        } else {
            searchResponse = getTodaySearch(todayQuery, userId);
        }
        return mapResponseToDto(searchResponse, todayQuery.getGeoPoint(), pageToken, userId);
    }

    private SearchResponse getMenusFromES(MenuQueryDto query) {
        QueryBuilder queryBuilder = StringUtils.isNotBlank(query.getKitchenId())
                ? QueryBuilders.matchQuery(ES_INDEX_FIELD_KITCHEN_ID, query.getKitchenId())
                : QueryBuilders.matchAllQuery();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(queryBuilder)
                .sort(ES_INDEX_FIELD_MENU_DATE, SortOrder.DESC)
                .size(Defaults.PAGE_SIZE);
        return elasticSearchService.searchMenuIndex(sourceBuilder, true);
    }


    private SearchResponse getTodaySearch(MenuTodayQueryDto todayQueryDto, String userId) {
        BoolQueryBuilder queryBuilder = buildTodayMenuBoolQuery(todayQueryDto, userId);
        SearchSourceBuilder sourceBuilder = buildMenuSourceBuilder(queryBuilder,
                null, todayQueryDto.getGeoPoint());
        sourceBuilder.size(todayQueryDto.getPageSize());
        return elasticSearchService.searchMenuIndex(sourceBuilder, true);
    }

    private SearchResponse searchAsNew(MenuSearchDto searchDto) {
        BoolQueryBuilder queryBuilder = buildMenuSearchBoolQuery(searchDto);
        HighlightBuilder highlightBuilder = buildMenuSearchHighlight();
        SearchSourceBuilder sourceBuilder = buildMenuSourceBuilder(queryBuilder, highlightBuilder, searchDto.getGeoPoint());
        sourceBuilder.size(searchDto.getPageSize());
        return elasticSearchService.searchMenuIndex(sourceBuilder, true);
    }

    private SearchSourceBuilder buildMenuSourceBuilder(BoolQueryBuilder searchBoolQuery,
                                                       HighlightBuilder highlightBuilder,
                                                       GeoPointDto geoPointDto) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        return sourceBuilder
                .query(searchBoolQuery)
                .highlighter(highlightBuilder)
                .sort("_score", SortOrder.DESC)
                .sort(ES_INDEX_FIELD_MENU_DATE, SortOrder.DESC)
                .sort(SortBuilders.geoDistanceSort(
                        ES_INDEX_FIELD_MENU_LOCATION,
                        new GeoPoint(geoPointDto.getLat(), geoPointDto.getLon()))
                        .ignoreUnmapped(true)
                        .order(SortOrder.ASC)
                        .sortMode(SortMode.MIN)
                        .unit(DistanceUnit.METERS)
                        .geoDistance(GeoDistance.ARC) // 'sloppy_arc' not available. This has impact on performance.
                );
    }

    private BoolQueryBuilder buildMenuSearchBoolQuery(MenuSearchDto searchDto) {

        return QueryBuilders.boolQuery()
                .must(buildMultiMatchQuery(searchDto.getSearchString()))
                .filter(buildGeoQuery(searchDto.getGeoPoint()));
    }

    private MultiMatchQueryBuilder buildMultiMatchQuery(String searchString) {
        MultiMatchQueryBuilder multiMatch = QueryBuilders.multiMatchQuery(searchString)
                .operator(Operator.OR)
                .field(ES_INDEX_FIELD_MENU_TEXT, 1.5f)
                .field(ES_INDEX_FIELD_BUSINESS_NAME, 3.5f);
        for (String field: getLanguageFields(ES_INDEX_FIELD_MENU_TEXT)) {
            multiMatch.field(field, 2.5f);
        }
        return multiMatch;
    }

    private HighlightBuilder buildMenuSearchHighlight() {
        HighlightBuilder builder = new HighlightBuilder()
                .field(ES_INDEX_FIELD_MENU_TEXT)
                .fragmentSize(30);
        for (String field: getLanguageFields(ES_INDEX_FIELD_MENU_TEXT) ) {
            builder.field(field);
        }
        return builder;
    }

    private BoolQueryBuilder buildTodayMenuBoolQuery(MenuTodayQueryDto todayQueryDto, String userId) {
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(todayQueryDto.getZoneOffsetSeconds());
        BoolQueryBuilder builder = QueryBuilders
                .boolQuery()
                .filter(QueryBuilders
                        .rangeQuery(ES_INDEX_FIELD_MENU_DATE)
                        .gte(DateUtil.zonedStartOfDay(zoneOffset))
                        .lte("now")
                        .timeZone(zoneOffset.getId()))
                .filter(buildGeoQuery(todayQueryDto.getGeoPoint()));

        //Boost user search result by mapped preferences using 'should' clause
        if (StringUtils.isNotBlank(userId)) {
            getAllUserKitchenPreferences(userId)
                    .forEach(p -> builder.should(buildMultiMatchQuery(p)));
        }
        return builder;
    }

    private GeoDistanceQueryBuilder buildGeoQuery(GeoPointDto geoPoint) {
        return QueryBuilders.geoDistanceQuery(ES_INDEX_FIELD_MENU_LOCATION)
                .point(geoPoint.getLat(), geoPoint.getLon())
                .ignoreUnmapped(true)
                .distance(queryMaxGeoDistanceInKm, DistanceUnit.KILOMETERS);
    }

    @SneakyThrows
    private MenuDto mapSearchHitToMenuDto(SearchHit hit, GeoPointDto pointOfInterest, String userId) {
        Menu menu = objectMapper.readValue(hit.getSourceAsString(), Menu.class);
        Map<String, HighlightField> highlightFields = hit.getHighlightFields();

        List<String> highlights = (highlightFields != null)
                ? highlightFields.values().stream().map(HighlightField::fragments)
                    .flatMap(Stream::of)
                    .map(Text::string).collect(Collectors.toList())
                : Collections.emptyList();
        return menuMapper.toMenuDto(menu, highlights, pointOfInterest, userId);
    }

    private Set<String> getAllUserKitchenPreferences(String userId) {
        return userKitchenMappingDao.getUserKitchenMappings(userId).stream()
                .map(UserKitchenMapping::getFoodPreferences)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private PaginatedContentDto<MenuDto> mapResponseToDto(SearchResponse searchResponse, GeoPointDto pointOfInterest,
                                                          String pageToken, String userId) {
        SearchHit[] searchHits;
        long totalCount;

        if (searchResponse.getHits() != null) {
            searchHits = searchResponse.getHits().getHits();
            totalCount = searchResponse.getHits().getTotalHits().value;
        } else {
            searchHits = new SearchHit[0];
            totalCount = 0;
        }
        return PaginatedContentDto.<MenuDto>builder()
                .nextPageToken(searchResponse.getScrollId())
                .pageToken(pageToken)
                .nextPageTokenTtl(keepPageTokenAlive)
                .count((long) searchHits.length)
                .totalCount(totalCount)
                .data(Arrays.stream(searchHits)
                        .map((hit) -> mapSearchHitToMenuDto(hit, pointOfInterest, userId))
                        .collect(Collectors.toList()))
                .build();
    }

    private Set<String> getLanguageFields(String prefix) {
       return Arrays.stream(Language.values()).map(l -> prefix + "-" +l.getValue()).collect(Collectors.toSet());
    }
}
