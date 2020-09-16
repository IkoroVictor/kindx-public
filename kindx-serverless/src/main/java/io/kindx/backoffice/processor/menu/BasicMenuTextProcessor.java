package io.kindx.backoffice.processor.menu;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.suffix.ConcurrentSuffixTree;
import io.kindx.constants.Defaults;
import io.kindx.util.TextUtil;
import lombok.Builder;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class BasicMenuTextProcessor implements MenuTextProcessor<String> {

    private static final List<Locale> DEFAULT_LOCALES =  Collections.singletonList(Locale.ENGLISH);

    private String text;
    private String sanitizedText;
    private List<Locale> locales;
    private Set<String> stopWords;
    private String lineDelimiterRegex ;
    private String wordDelimiterRegex;

    private boolean filterOutStopWordsWhenScoring;
    private boolean bruteForceFallbackIfNecessary;


    private Map<String, List<Integer>> wordLineNumbersMap;
    private Map<String, List<Integer>> wordIndexInLineMap;
    private ConcurrentSuffixTree<Integer> wordSuffixTree;

    @Builder
    public BasicMenuTextProcessor(String text,
                                  @Singular List<Locale> locales,
                                  List<String> stopWords,
                                  String lineDelimiterRegex,
                                  String wordDelimiterRegex,
                                  boolean filterOutStopWordsWhenScoring,
                                  boolean bruteForceFallbackIfNecessary) {
        this.text = text;
        this.locales = locales;
        this.stopWords = getSanitizedStopWords(stopWords);
        this.lineDelimiterRegex = lineDelimiterRegex;
        this.wordDelimiterRegex = wordDelimiterRegex;
        this.filterOutStopWordsWhenScoring = filterOutStopWordsWhenScoring;
        this.bruteForceFallbackIfNecessary = bruteForceFallbackIfNecessary;
        wordLineNumbersMap = new HashMap<>();
        wordIndexInLineMap = new HashMap<>();
        wordSuffixTree = new ConcurrentSuffixTree<>(new DefaultCharArrayNodeFactory());
        process();
    }


    @Override
    public float score(String itemText) {
        if(StringUtils.isBlank(itemText)) {
            throw new IllegalArgumentException("Text cannot be blank.");
        }
        String sanitizedItemText = sanitizeText(itemText);
        float score = scoreText(sanitizedItemText);
        if (score == 0.0f && bruteForceFallbackIfNecessary) {
            score = sanitizedText.contains(sanitizedItemText) ? 1.0f : score;
        }
        return score;
    }

    @Override
    public List<ScoreResult> scores(String itemText) {
        return Collections.singletonList(ScoreResult.builder()
                .score(scoreText(itemText)).build());
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

    private float scoreText(String systemFriendlyText) {
        String[] words = Arrays.stream(systemFriendlyText.split(Defaults.SYSTEM_TEXT_DELIMITER_REGEX))
                .filter(StringUtils::isNotBlank)
                .filter(word -> !filterOutStopWordsWhenScoring || !stopWords.contains(word))
                .toArray(String[]::new);

        if (words.length == 0) {
            return 0;
        }

        float totalScore = 0;

        float maxWordScore = 1.0f/words.length;
        float midWordScore = maxWordScore/2;
        float lowWordScore = midWordScore/2;

        totalScore += wordLineNumbersMap.containsKey(words[0])
                ? maxWordScore
                : (wordProbablyInTree(words[0]) ? midWordScore : 0.0f);

        if (words.length == 1) {
            return totalScore;
        }

        for (int i = 1; i < words.length ; i++) {
            String word =  words[i];
            String prevWord = words[i-1];
            if (wordLineNumbersMap.containsKey(word) || wordProbablyInTree(word)) {
                float score = lowWordScore;
                if (wordIndexInLineMap.containsKey(word)) {
                    //Word is actually a key not a substring of a key....
                    List<Integer> wordLineNumbers =  wordLineNumbersMap.get(word);
                    List<Integer> prevWordLineNumbers = null;
                    if(!wordIndexInLineMap.containsKey(prevWord)) {
                        //Previous word not in menu, give max word score to prevent 'first word' score bias.
                        totalScore += maxWordScore;
                        continue;
                    }
                    prevWordLineNumbers =  wordLineNumbersMap.get(prevWord);
                    for (int j = 0; j <  wordLineNumbers.size() && prevWordLineNumbers != null; j++) {
                        int sameLineNumberIndex =  prevWordLineNumbers.indexOf(wordLineNumbers.get(j));

                        if (sameLineNumberIndex != -1) {
                            Integer wordIndexInLine = wordIndexInLineMap.get(word).get(j);
                            Integer prevWordIndexInLine = wordIndexInLineMap.get(prevWord).get(sameLineNumberIndex);
                            if ( (wordIndexInLine - prevWordIndexInLine) == 1) {
                                score = maxWordScore;
                                break;
                            }
                            if ((wordIndexInLine - prevWordIndexInLine) > 1) {
                                //Word is after previous word in line
                                score = Math.max(midWordScore, score);
                                break;
                            }
                        }
                    }
                }
                totalScore += score;
            }
        }

        return totalScore;

    }


    private void process() {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException("'text' cannot be empty.");
        }

        if (bruteForceFallbackIfNecessary) {
            sanitizedText = sanitizeText(text);
        }
        this.locales = locales == null || locales.isEmpty() ? DEFAULT_LOCALES : locales;
        this.lineDelimiterRegex = StringUtils.isBlank(lineDelimiterRegex) ? Defaults.LINE_DELIMITER_REGEX : lineDelimiterRegex;
        this.wordDelimiterRegex = StringUtils.isBlank(wordDelimiterRegex) ? Defaults.WORD_DELIMITER_REGEX : wordDelimiterRegex;
        processText();
    }


    private void processText() {
        String[] lines =  text.split(lineDelimiterRegex);
        for (int i = 0; i < lines.length; i++) {
            String sanitizedLine = sanitizeText(lines[i]);
            String[] words = sanitizedLine.split(wordDelimiterRegex);
            int j = 0;
            for (String processed : words) {
                if(StringUtils.isNotBlank(processed) && !stopWords.contains(processed)) {
                    wordSuffixTree.put(processed, j+1); //1-based index;
                    wordLineNumbersMap.putIfAbsent(processed, new ArrayList<>());
                    wordIndexInLineMap.putIfAbsent(processed, new ArrayList<>());

                    final int lineIndex = i;
                    final int wordIndexInline= j;

                    wordLineNumbersMap.computeIfPresent(processed,  (key, list)-> {list.add(lineIndex); return list;});
                    wordIndexInLineMap.computeIfPresent(processed,  (key, list)-> {list.add(wordIndexInline); return list;});
                    j++;
                }

            }
        }
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
        return TextUtil.toLocalesLowerCase(TextUtil.toSystemFriendlyText(text).trim(), locales);
    }

    private boolean wordProbablyInTree(String word) {
        return wordSuffixTree.getValuesForKeysContaining(word).iterator().hasNext();
    }

}
