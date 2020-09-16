package io.kindx.backoffice.processor.menu.es;

import io.kindx.backoffice.processor.menu.MenuTextProcessor;
import io.kindx.constants.Language;
import io.kindx.dto.GeoPointDto;
import io.kindx.elasticsearch.ElasticSearchService;
import lombok.Builder;
import lombok.Getter;
import org.elasticsearch.client.indices.AnalyzeResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ESMenuProcessor implements MenuTextProcessor<String> {
    protected ElasticSearchService service;

    protected ESMenuProcessor(ElasticSearchService service) {
        this.service = service;
    }

    public float score () { throw  new UnsupportedOperationException("Not implemented");}

    public abstract List<ScoreResult> scores(Request request);

    @Override
    public String analyze(String textToAnalyze) {
        return analyze(new String[]{textToAnalyze})[0];
    }

    @Override
    public String[] analyze(String... textsToAnalyze) {
        List<AnalyzeResponse.AnalyzeToken> tokens = service.analyzeAsciiFolded(textsToAnalyze)
                .getTokens();

        String[] analyzed = new String[textsToAnalyze.length];
        int i = 0;
        int totalLength = textsToAnalyze[0].length() + 1; //1 for space added by ES for multi-value analysis
        List<String> termHolder = new ArrayList<>();
        for (AnalyzeResponse.AnalyzeToken token : tokens) {
            if(token.getStartOffset() >= totalLength) {
                analyzed[i] = String.join(" ", termHolder.toArray(new String[0]));
                termHolder.clear();
                i++;
                if(i < textsToAnalyze.length) {
                    totalLength += textsToAnalyze[i].length() + 1;
                }
            }
            termHolder.add(token.getTerm());
        }
        if (!termHolder.isEmpty()){
            analyzed[textsToAnalyze.length - 1] = String.join(" ", termHolder.toArray(new String[0]));
        }
        return analyzed;
    }

    @Builder
    @Getter
    public static class Request {
        private String textToScore;
        private long distanceInKm;
        private Collection<Language> languages;
        private GeoPointDto point;
        private String menuId;
    }
}
