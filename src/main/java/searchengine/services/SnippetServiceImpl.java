package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SnippetServiceImpl implements SnippetService {
    private static final int SNIPPET_LENGTH = 200;
    private final TextUtils textUtils;
    private final LemmaService lemmaService;

    @Override
    public String getSnippet(String html, String[] queryLemmas) {
        String text = textUtils.removeHtmlTags(html);
        String firstQueryWord = getFirstOccurrenceQueryWord(text, queryLemmas);
        int firstQueryItemPosition = firstQueryWord == null ? -1 : text.indexOf(firstQueryWord);
        String truncatedText = truncate(text, firstQueryItemPosition);
        return getHighlightedText(truncatedText, queryLemmas);
    }

    private String getFirstOccurrenceQueryWord(String text, String[] queryLemmas) {
        String[] words = textUtils.getWordsFromText(text);
        for (String word : words) {
            if (isContainedInQuery(word, queryLemmas)) {
                return word;
            }
        }
        return null;
    }

    private boolean isContainedInQuery(String word, String[] queryLemmas) {
        word = word.toLowerCase();
        for (String lemma : queryLemmas) {
            if ((word.contains(lemma.substring(0, lemma.length() - 1))) &&
                    lemmaService.getLemmasStatistics(word).containsKey(lemma)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String text, int queryItemPosition) {
        int beginPosition = queryItemPosition > 0 ? getBeginPosition(text, queryItemPosition) : 0;
        return text.substring(beginPosition, getEndPosition(text, beginPosition));
    }

    private int getBeginPosition(String text, int queryItemPosition) {
        String substring = text.substring(0, queryItemPosition);
        return Stream.of(substring.lastIndexOf(".") + 1,
                        substring.lastIndexOf("!") + 1,
                        substring.lastIndexOf("?") + 1)
                .filter(i -> i > 0 && i > queryItemPosition - SNIPPET_LENGTH / 2)
                .max(Integer::compareTo)
                .orElse(substring.indexOf(" ", queryItemPosition - SNIPPET_LENGTH / 2));
    }

    private int getEndPosition(String text, int beginPosition) {
        int fromIndex = beginPosition + SNIPPET_LENGTH;
        int toIndex = fromIndex + 80;
        return Stream.of(text.indexOf(".", fromIndex) + 1,
                        text.indexOf("!", fromIndex) + 1,
                        text.indexOf("?", fromIndex) + 1)
                .filter(i -> i > 0 && i < toIndex)
                .min(Integer::compareTo)
                .orElse(text.indexOf(" ", toIndex));
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
