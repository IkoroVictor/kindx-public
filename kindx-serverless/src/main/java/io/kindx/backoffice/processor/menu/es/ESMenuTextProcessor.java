package io.kindx.backoffice.processor.menu.es;

import io.kindx.backoffice.processor.menu.MenuTextProcessor;
import io.kindx.constants.Language;
import io.kindx.dto.GeoPointDto;
import io.kindx.elasticsearch.ElasticSearchService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ESMenuTextProcessor extends ESMenuProcessor {

    private static final String ES_MENU_ID_FIELD = "menuId";
    private static final String ES_MENU_TEXT_FIELD = "menuText";
    private static final String ES_SCORE_FIELD = "_score";
    private static final String ES_MENU_DATE_FIELD = "menuDate";
    private static final String ES_MENU_LOCATION_FIELD = "location.geoPoint";

    private static final Logger logger = LogManager.getLogger(ESMenuTextProcessor.class);

    private static final long DEFAULT_SEARCH_DISTANCE_KM = 20;

    public ESMenuTextProcessor(ElasticSearchService service) {
        super(service);
        this.service = service;
    }

    @Override
    public float score(String itemText) {
        return scores(itemText).get(0).getScore();
    }

    @Override
    public List<ScoreResult> scores(String toScore) {
        return scores(Request.builder()
                .textToScore(toScore)
                .distanceInKm(DEFAULT_SEARCH_DISTANCE_KM)
                .languages(Arrays.asList(Language.values()))
                .menuId(null)
                .point(null)
                .build());
    }

    @Override
    public List<ScoreResult> scores(Request request) {
        return scores(request.getTextToScore(),
                request.getDistanceInKm(),
                request.getLanguages(),
                request.getPoint(),
                request.getMenuId());
    }

    private List<ScoreResult> scores(String textToScore,
                                    long distanceInKm,
                                    Collection<Language> languages,
                                    GeoPointDto point,
                                    String menuId) {
        return mapToScoreResults(search(textToScore, languages, distanceInKm,  point, menuId));
    }


    private SearchResponse search(String scoreText,
                                  Collection<Language> languages,
                                  long distanceRadiusKm,
                                  @Nullable GeoPointDto point,
                                  @Nullable String menuId) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        HighlightBuilder highlightBuilder = new HighlightBuilder().fragmentSize(30);

        for (Language l : languages) {
            String field = String.format("%s-%s", ES_MENU_TEXT_FIELD, l.getValue());
            builder.should(QueryBuilders.matchQuery(field, scoreText).operator(Operator.AND));
            highlightBuilder.field(field);
        }

        if (StringUtils.isNotBlank(menuId)) {
            builder.must(QueryBuilders.matchQuery(ES_MENU_ID_FIELD, menuId));
        }

        if (point != null) {
            builder.filter(buildGeoQuery(point, distanceRadiusKm));
            sourceBuilder.sort(SortBuilders.geoDistanceSort(
                    ES_MENU_LOCATION_FIELD,
                    new GeoPoint(point.getLat(), point.getLon()))
                    .ignoreUnmapped(true)
                    .order(SortOrder.ASC)
                    .sortMode(SortMode.MIN)
                    .unit(DistanceUnit.METERS)
                    .geoDistance(GeoDistance.ARC) // 'sloppy_arc' not available. This has impact on performance.
            );
        }

        sourceBuilder.query(builder)
                .fetchSource(false) //To reduce payload size
                .size(10)//Top 10
                .highlighter(highlightBuilder)
                .sort(ES_SCORE_FIELD, SortOrder.DESC)
                .sort(ES_MENU_DATE_FIELD, SortOrder.DESC);

        return service.searchMenuIndex(sourceBuilder, false);
    }

    private GeoDistanceQueryBuilder buildGeoQuery(GeoPointDto geoPoint, long distanceInKm) {
        return QueryBuilders.geoDistanceQuery(ES_MENU_ID_FIELD)
                .point(geoPoint.getLat(), geoPoint.getLon())
                .ignoreUnmapped(true)
                .distance(distanceInKm, DistanceUnit.KILOMETERS);
    }

    @SneakyThrows
    private List<MenuTextProcessor.ScoreResult> mapToScoreResults(SearchResponse response) {
        List<SearchHit> topHits = Collections.emptyList();
        if (response.getHits() != null && response.getHits().getTotalHits().value != 0) {
            topHits = Stream.of(response.getHits().getHits())
                    //TODO: Hack fix for 'menuText' field that returns no highlight. Find way to exclude field from search query
                    .filter(h -> !h.getHighlightFields().isEmpty())
                    .collect(Collectors.toList());
        }

        if(topHits.isEmpty()) {
            return Collections.singletonList(MenuTextProcessor.ScoreResult.builder()
                    .line(null)
                    .textId(null)
                    .score(0)
                    .lineNumber(-1)
                    .build());
        }

        List<ScoreResult> results = new ArrayList<>();
        for (SearchHit hit : topHits) {
            //First highlighted fragment
            List<String> highlights = hit.getHighlightFields().values().stream()
                    .map(HighlightField::fragments)
                    .flatMap(Stream::of)
                    .map(Text::string).collect(Collectors.toList());
            results.add(ScoreResult.builder()
                    .lineNumber(-1)
                    .textId(hit.getId())
                    .line(highlights.get(0))
                    .score(1)
                    .build());
        }

        return results;
    }

}
