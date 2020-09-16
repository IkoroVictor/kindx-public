package io.kindx.backoffice.processor.menu.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kindx.constants.Language;
import io.kindx.elasticsearch.ElasticSearchService;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.*;

public class ESMenuLineProcessor extends ESMenuProcessor {

    public static final String ES_INDEX_TEXT_ID_FIELD = "textId";
    public static final String ES_INDEX_LINE_FIELD = "line";

    @NonNull
    private String textId;
    private final String text;
    private final List<Language> languages;
    private final String lineDelimiterRegex;
    private ObjectMapper objectMapper;

    @Builder
    public ESMenuLineProcessor(ElasticSearchService service,
                               String textId,
                               String text,
                               @Singular List<Language> languages,
                               String lineDelimiterRegex, ObjectMapper objectMapper) {
        super(service);
        this.service = service;
        this.textId = textId;
        this.text = text;
        this.languages = languages;
        this.lineDelimiterRegex = lineDelimiterRegex;
        this.objectMapper = objectMapper;
        process();
    }

    private void process() {
        validate();
        List<Language> noLinesLanguages = new ArrayList<>();
        languages.forEach(language -> {
            if (!service.getLineFromLineIndex(textId, language).isExists()) {
                noLinesLanguages.add(language);
            }
        });
        if (!noLinesLanguages.isEmpty() && StringUtils.isNotBlank(text)) {
            loadLinesToIndex(noLinesLanguages);
        }
    }
    private void loadLinesToIndex(Collection<Language> languageCollection) {
        String[] originalLines = text.split(lineDelimiterRegex);
        Map<String, Line> linesMap = new HashMap<>();
        for (int i = 0; i < originalLines.length; i++) {
            if(StringUtils.isNotBlank(originalLines[i])) {
                Line line = mapLine(originalLines[i], i);
                linesMap.put(line.getLineId(), line);
            }
        }
        //Base line
        linesMap.put(textId, Line.builder()
                .createdTimestamp(new Date().getTime())
                .lineNumber(-1)
                .textId(textId).build());
        service.putInLinesIndex(linesMap, languageCollection.toArray(new Language[0]));
    }

    private Line mapLine(String line, int lineNumber) {
        return Line.builder()
                .createdTimestamp(new Date().getTime())
                .lineNumber(lineNumber)
                .line(line)
                .textId(textId)
                .lineId(textId + "_" + lineNumber)
                .build();
    }

    private void validate() {
        if (StringUtils.isBlank(textId)) {
            throw new IllegalArgumentException("'textId' cannot be null or   empty.");
        }
        if (languages == null || languages.isEmpty()) {
            throw new IllegalArgumentException("At least one language required");
        }
    }


    @Override
    public float score(String itemText) {
        return scores(itemText).get(0).getScore();
    }

    @Override
    public List<ScoreResult> scores(String textToScore) {
        return mapToScoreResults(search(textToScore));
    }

    @Override
    public List<ScoreResult> scores(Request request) {
        return scores(request.getTextToScore());
    }



    private SearchResponse search(String scoreText) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilder builder =  QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery(ES_INDEX_LINE_FIELD, scoreText).operator(Operator.AND))
                .must(QueryBuilders.matchQuery(ES_INDEX_TEXT_ID_FIELD, textId));
        sourceBuilder.query(builder);
        return service.searchLinesIndex(sourceBuilder, languages.toArray(new Language[0]));
    }

    @SneakyThrows
    private List<ScoreResult> mapToScoreResults(SearchResponse response) {
        if (response.getHits() == null || response.getHits().getTotalHits().value == 0) {
            return Collections.singletonList( ScoreResult.builder()
                    .line(null)
                    .score(0)
                    .lineNumber(-1)
                    .build());
        }
        SearchHit[] hits = response.getHits().getHits();
        List<ScoreResult> results = new ArrayList<>();
        Set<String> lineIdSet = new HashSet<>();
        for (SearchHit hit : hits) {
            Line line = objectMapper.readValue(hit.getSourceAsString(), Line.class);
            if (!lineIdSet.contains(line.getLineId())) { //Filter for duplicate results across indices
                results.add(ScoreResult.builder()
                        .lineNumber(line.getLineNumber())
                        .line(line.getLine())
                        .score(1)
                        .build());
                lineIdSet.add(line.getLineId());
            }
        }
        return results;
    }


    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Line {
        private String line;
        private String lineId;
        private String textId;
        private int lineNumber;
        private long createdTimestamp;
    }
}
