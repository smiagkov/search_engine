package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {
    public static final String RESTRICTED_LEMMA_TYPES_PATTERN = ".+(ПРЕДЛ|СОЮЗ|МЕЖД|ЧАСТ).*";
    private final TextUtils textUtils;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private LuceneMorphology luceneMorphology;

    private static final Object lock = new Object();

    @Override
    public void updateOnDeletePage(PageEntity page) {
        List<LemmaEntity> lemmas = lemmaRepository.findAllByPage(page);
        lemmaRepository.decrementFrequencyOrDelete(lemmas);
    }

    @Override
    public void addLemmasAndIndexesToDB(SiteEntity site, PageEntity page){
        Map<String, Integer> lemmas = getLemmasStatistics(textUtils.removeHtmlTags(page.getContent()));
        for (Map.Entry<String, Integer> lemmaEntry : lemmas.entrySet()) {
            LemmaEntity lemma = addOrUpdateLemma(site, lemmaEntry);
            indexRepository.save(new IndexEntity(page, lemma, lemmaEntry.getValue()));
        }
    }

    private LemmaEntity addOrUpdateLemma(SiteEntity site, Map.Entry<String, Integer> lemmaEntry) {
        LemmaEntity lemma;
        synchronized (lock) {
            Optional<LemmaEntity> optionalLemma = lemmaRepository.findBySiteLemma(site, lemmaEntry.getKey());
            if (optionalLemma.isEmpty()) {
                lemma = new LemmaEntity(site, lemmaEntry.getKey());
            } else {
                lemma = optionalLemma.get();
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            lemmaRepository.save(lemma);
        }
        return lemma;
    }

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
