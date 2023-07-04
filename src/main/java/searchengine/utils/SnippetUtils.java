package searchengine.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SnippetUtils {
    private static final int SNIPPET_LENGTH = 200;
    private final TextUtils textUtils;
    private final LemmaUtils lemmaUtils;

    public String getSnippet(String html, String[] queryLemmas) {
        String text = textUtils.removeHtmlTags(html);
        int firstQueryWordPosition = getFirstOccurrenceQueryWordPosition(text, queryLemmas);
        String truncatedText = truncate(text, firstQueryWordPosition);
        return getHighlightedText(truncatedText, queryLemmas);
    }

    private int getFirstOccurrenceQueryWordPosition(String text, String[] queryLemmas) {
        String[] words = textUtils.getWordsFromText(text);
        for (String word : words) {
            if (isContainedInQuery(word, queryLemmas)) {
                return text.indexOf(word);
            }
        }
        return -1;
    }

    private boolean isContainedInQuery(String word, String[] queryLemmas) {
        word = word.toLowerCase();
        for (String lemma : queryLemmas) {
            if (lemmaUtils.getLemmasStatistics(word).containsKey(lemma)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String text, int queryItemPosition) {
        if (queryItemPosition <= 0) {
            return text.substring(0, Math.min(text.length(), SNIPPET_LENGTH));
        }
        int beginPosition = getBeginPosition(text, queryItemPosition);
        return text.substring(beginPosition, getEndPosition(text, beginPosition));
    }

    private int getBeginPosition(String text, int queryItemPosition) {
        String substring = text.substring(0, queryItemPosition);
        int startPosition = Math.max(0, queryItemPosition - SNIPPET_LENGTH / 2);
        Optional<Integer> beforeDelimiterPosition = Stream.of(substring.lastIndexOf(".") + 1,
                        substring.lastIndexOf("!") + 1,
                        substring.lastIndexOf("?") + 1)
                .filter(i -> i > 0 && i > startPosition)
                .max(Integer::compareTo);
        return beforeDelimiterPosition.orElseGet(() -> Math.max(0, text.indexOf(" ", startPosition)));
    }

    private int getEndPosition(String text, int beginPosition) {
        int toIndex = beginPosition + SNIPPET_LENGTH;
        if (toIndex >= text.length()){
            return text.length();
        }
        int fromIndex = toIndex - 50;
        Optional<Integer> optionalEndPosition = Stream.of(text.indexOf(".", fromIndex) + 1,
                        text.indexOf("!", fromIndex) + 1,
                        text.indexOf("?", fromIndex) + 1)
                .filter(i -> i > 0 && i <= toIndex + 50)
                .max(Integer::compareTo);
        return optionalEndPosition.orElseGet(() -> text.indexOf(" ", fromIndex) + 1);
    }

    private String getHighlightedText(String text, String[] queryLemmas) {
        String[] wordsToHighlight = Arrays.stream(textUtils.getWordsFromText(text))
                .distinct()
                .filter(i -> isContainedInQuery(i, queryLemmas))
                .toArray(String[]::new);
        for (String word : wordsToHighlight) {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        }
        return text;
    }
}
