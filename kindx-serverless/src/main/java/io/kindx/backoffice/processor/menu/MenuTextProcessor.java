package io.kindx.backoffice.processor.menu;

import lombok.Builder;
import lombok.Data;

import java.util.List;

public interface MenuTextProcessor<T> {

    float score(String textToScore);

    List<ScoreResult> scores(T toScore);

    T analyze(T toAnalyze);

    T[] analyze(T[] toAnalyze);

    @Builder
    @Data
    class ScoreResult {
        private float score;
        private int lineNumber;
        private String textId;
        private String line ;
    }

}
