package searchengine.utils;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LemmaUtils {
    public static final String RESTRICTED_LEMMA_TYPES_PATTERN = ".+(ПРЕДЛ|СОЮЗ|МЕЖД|ЧАСТ).*";
    private final TextUtils textUtils;
    private LuceneMorphology luceneMorphology;

    public Map<String, Integer> getLemmasStatistics(String text) {
        Map<String, Integer> lemmas;
        String[] words = textUtils.getWordsFromText(text);
        lemmas = Arrays.stream(words)
                .map(String::toLowerCase)
                .map(getLuceneMorphology()::getMorphInfo)
                .flatMap(List::stream)
                .filter(Predicate.not(s -> s.matches(RESTRICTED_LEMMA_TYPES_PATTERN)))
                .map(s -> s.split("\\|"))
                .collect(Collectors.toMap(i -> i[0], i -> 1, Integer::sum));
        return lemmas;
    }

    private LuceneMorphology getLuceneMorphology() {
        if (luceneMorphology == null) {
            try {
                luceneMorphology = new RussianLuceneMorphology();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return luceneMorphology;
    }
}
