package io.kindx.backoffice.processor.menu;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.suffix.ConcurrentSuffixTree;
import io.kindx.constants.Defaults;
import io.kindx.util.TextUtil;
import lombok.Builder;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SuffixTreeMenuTextProcessor implements MenuTextProcessor<String> {


    private static final List<Locale> DEFAULT_LOCALES =  Collections.singletonList(Locale.ENGLISH);

    private String text;
    private List<Locale> locales;
    private Set<String> stopWords;
    private String lineDelimiterRegex ;
    private String wordDelimiterRegex;

    private String[] originalLines;
    private Map<String, List<Integer>> wordLineIndexMap;
    private Map<Integer, List<int[]>> lineSubSentencesIndexMap;
    private ConcurrentSuffixTree<Integer> linesSuffixTree;

    private LevenshteinDistance levenshteinDistance;


    @Builder
    public SuffixTreeMenuTextProcessor(String text,
                                       @Singular List<Locale> locales,
                                       List<String> stopWords,
                                       String lineDelimiterRegex,
                                       String wordDelimiterRegex) {
        this.text = text;
        this.locales = locales;
        this.stopWords = getSanitizedStopWords(stopWords);
        this.lineDelimiterRegex = lineDelimiterRegex;
        this.wordDelimiterRegex = wordDelimiterRegex;
        originalLines = new String[0];
        wordLineIndexMap = new HashMap<>();
        lineSubSentencesIndexMap = new HashMap<>();
        linesSuffixTree = new ConcurrentSuffixTree<>(new DefaultCharArrayNodeFactory());
        levenshteinDistance = LevenshteinDistance.getDefaultInstance();
        process();
    }

    private void process() {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException("'text' cannot be empty.");
        }

        this.locales = locales == null || locales.isEmpty() ? DEFAULT_LOCALES : locales;
        this.lineDelimiterRegex = StringUtils.isBlank(lineDelimiterRegex) ? Defaults.LINE_DELIMITER_REGEX : lineDelimiterRegex;
        this.wordDelimiterRegex = StringUtils.isBlank(wordDelimiterRegex) ? Defaults.WORD_DELIMITER_REGEX : wordDelimiterRegex;
        processText();
    }


    @Override
    public float score(String itemText) {
        return scores(itemText).get(0).getScore();
    }

    @Override
    public List<ScoreResult> scores(String itemText) {
        if(StringUtils.isBlank(itemText)) {
            throw new IllegalArgumentException("Text cannot be blank");
        }
        String sanitizedItemText = sanitizeText(itemText);
        return textScores(sanitizedItemText);
    }

    @Override
    public String analyze(String textToAnalyze) {
        return analyze(new String[]{textToAnalyze})[0];
    }

    @Override
    public String[] analyze(String... textsToAnalyze) {
        return Stream.of(textsToAnalyze)
                .map(this::sanitizeText)
                .toArray(String[]::new);
    }

    private List<ScoreResult> textScores(String sanitizedText) {
        float fullScore = 1.0f;
        float partialSuffixScore = 0.75f;
        String[] words = Arrays.stream(sanitizedText.split(Defaults.SYSTEM_TEXT_DELIMITER_REGEX))
                .filter(StringUtils::isNotBlank)
                .filter(word -> !stopWords.contains(word))
                .toArray(String[]::new);


        if (words.length == 0) {
            return notFoundResultList();
        }

        if (words.length == 1) {
            List<ScoreResult> results;
            if (wordLineIndexMap.containsKey(words[0])) {
                results = wordLineIndexMap.get(words[0])
                        .stream()
                        .map((index) -> buildScoreResult(fullScore, index))
                        .collect(Collectors.toList());
            } else {
                Iterable<Integer> treeResults = linesSuffixTree.getValuesForKeysContaining(words[0]);
                if (treeResults.iterator().hasNext()) {
                    final List<ScoreResult> scores =  new ArrayList<>();
                    treeResults.forEach(lineNumber -> scores.add(buildScoreResult(partialSuffixScore, lineNumber - 1)));
                    results = scores;
                } else {
                    results = notFoundResultList();
                }
            }
            return results;
        }

        //Multi-word search
        String combinedWords = String.join(Defaults.SYSTEM_TEXT_DELIMITER_REGEX, words);

        Iterable<Integer> treeResults = linesSuffixTree.getValuesForKeysContaining(combinedWords);
        if (treeResults.iterator().hasNext()) {
            List<ScoreResult> results =  new ArrayList<>();
            treeResults.forEach(lineNumber -> results.add(buildScoreResult(fullScore, lineNumber - 1)));
            return results;
        }
        return distanceScores(words, combinedWords);
    }


    private  List<ScoreResult> distanceScores(String[] words, String combinedWords) {
        //No direct match suffix tree
        Map<Integer, Integer> lineOccurrenceMap = new HashMap<>();

        for (String word : words) {
            Iterable<Integer> linesOfOccurrence = wordLineIndexMap.containsKey(word)
                    ? wordLineIndexMap.get(word)
                    : linesSuffixTree.getValuesForKeysContaining(word);

            for (Integer lineNumber : linesOfOccurrence) {
                lineOccurrenceMap.putIfAbsent(lineNumber, 0);
                lineOccurrenceMap.computeIfPresent(lineNumber, (k,v) -> ++v);
            }
        }
        if (lineOccurrenceMap.isEmpty()) {
            //Words don't exist in text
            return notFoundResultList();
        }

        List<Map.Entry<Integer, Integer>> list  =  new ArrayList<>(lineOccurrenceMap.entrySet());

        //Sort descending order of occurrence for optimisation. Line with most word intersections most likely to get the best score
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        return list.stream()
                .map(entry -> distanceBasedScoreResultFromLineSubSentences(entry.getKey(), combinedWords))
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore())) //Sort score descending
                .collect(Collectors.toList());
    }



    private void processText() {
        originalLines = text.split(lineDelimiterRegex);
        for (int i = 0; i < originalLines.length; i++) {
            String sanitizedLine = sanitizeText(originalLines[i]);
            if (StringUtils.isBlank(sanitizedLine) || StringUtils.isWhitespace(sanitizedLine)) {
                continue;
            }
            linesSuffixTree.put(sanitizedLine, i);
            String[] words = sanitizedLine.split(wordDelimiterRegex);
            for (String s : words) {
                if(StringUtils.isNotBlank(s) && !stopWords.contains(s)) {
                    String sanitizedWord = s.trim();
                    wordLineIndexMap.putIfAbsent(sanitizedWord, new ArrayList<>());
                    final int lineIndex = i;
                    wordLineIndexMap.computeIfPresent(sanitizedWord,  (key, list)-> {list.add(lineIndex); return list;});
                }
            }
            lineSubSentencesIndexMap.put(i, buildSubSentencesIndex(sanitizedLine, wordDelimiterRegex));
        }
    }

    private List<int[]> buildSubSentencesIndex(String sanitizedLine, String wordDelimiter) {
        List<int[]> indexes = new ArrayList<>();
        String[] sanitizedLineWords = sanitizedLine.split(wordDelimiter);

        int shiftFromIndex = 0;
        for (int i = 0; i < sanitizedLineWords.length; i++) {
            int shiftToIndex = 0;
            int indexFrom = sanitizedLine.indexOf(sanitizedLineWords[i], shiftFromIndex);
            for (int j = i; j < sanitizedLineWords.length; j++) {
                int indexTo = sanitizedLine.indexOf(sanitizedLineWords[j], (shiftFromIndex + shiftToIndex)) + sanitizedLineWords[j].length();
                indexes.add(new int[] {indexFrom, indexTo});
                shiftToIndex = indexTo - shiftFromIndex;
            }
            shiftFromIndex = indexFrom + sanitizedLineWords[i].length();
        }
        indexes.sort(Comparator.comparingInt(ints -> ints[1] - ints[0]));
        return indexes;
    }

    private Set<String> getSanitizedStopWords(List<String> stopWords) {
        if(stopWords == null || stopWords.isEmpty()) {
            return Collections.emptySet();
        }
        return stopWords
                .stream()
                .map(this::sanitizeText)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String sanitizeText(String text) {
        return toLocalesLowerCase(TextUtil.toSystemFriendlyText(text).trim());
    }

    private String toLocalesLowerCase(final String text) {
        String lowerCaseText = text;
        for (Locale locale : locales) {
            lowerCaseText = lowerCaseText.toLowerCase(locale);
        }
        return lowerCaseText;
    }

    private List<ScoreResult> notFoundResultList() {
        return Collections.singletonList(ScoreResult
                .builder()
                .score(0)
                .lineNumber(-1)
                .build());
    }

    private ScoreResult buildScoreResult(float score, int lineIndex) {
       return ScoreResult
                .builder()
                .score(score)
                .line(originalLines[lineIndex])
                .lineNumber(lineIndex + 1)
               .build();
    }

    private ScoreResult distanceBasedScoreResultFromLineSubSentences(int lineIndex, String combinedWords) {
        float resultScore = 0.0f;
        String sanitizedLine = sanitizeText(originalLines[lineIndex]);
        int wordsLength = combinedWords.length();
        for (int[] subSentenceIndex : lineSubSentencesIndexMap.get(lineIndex)) {
            String subSentence = sanitizedLine.substring(subSentenceIndex[0], subSentenceIndex[1]);
            int distance = levenshteinDistance.apply(combinedWords, subSentence);
            //Default to score 0 if distance is greater than searched word
            float score = wordsLength >= distance
                    ? (wordsLength - distance) / (float) wordsLength
                    : 0.0f;
            if (score > resultScore) {
                resultScore = score;
            }
        }
        return buildScoreResult(resultScore, lineIndex);
    }
}
