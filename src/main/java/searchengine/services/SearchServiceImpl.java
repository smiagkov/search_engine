package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.dto.search.*;
import searchengine.exceptions.EmptyQueryException;
import searchengine.exceptions.NotIndexedSiteException;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaService lemmaService;
    private final SnippetService snippetService;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final PageParsingUtils pageParsingUtils;

    private Query previousQuery;
    private Map<PageEntity, Float> previousSearchPagesWithRelevance;
    private final static float INFREQUENCY_FACTOR = 0.5F;

    public SearchResponse getQueryResponse(Query query) {
        String site = query.site();
        String queryText = query.queryText();
        Map<PageEntity, Float> pagesWithRelevance;
        String[] queryLemmas = getQueryLemmas(queryText);
        Arrays.stream(queryLemmas).forEach(i -> System.out.println(queryText + " -> " + i));
        try {
            if (queryText.isBlank()) {
                throw new EmptyQueryException("Задан пустой поисковый запрос");
            } else if (query.equals(previousQuery)) {
                pagesWithRelevance = Map.copyOf(previousSearchPagesWithRelevance);
            } else {
                pagesWithRelevance = getSearchResults(query, queryLemmas);
                previousQuery = query;
                previousSearchPagesWithRelevance = pagesWithRelevance;
            }
            return generateResponse(pagesWithRelevance, query, queryLemmas);
        } catch (EmptyQueryException e) {
            return new SearchErrorResponse(false, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (NotIndexedSiteException e) {
            String message = site == null ?
                    "Ни один из сайтов не проиндексирован" :
                    "Сайт " + site + " не проиндексирован";
            return new SearchErrorResponse(false, HttpStatus.METHOD_NOT_ALLOWED, message);
        }
    }

    private String[] getQueryLemmas(String queryText) {
        return lemmaService.getLemmasStatistics(queryText).keySet().toArray(String[]::new);
    }

    private SiteEntity[] getSites(Query query) throws RuntimeException {
        SiteEntity[] siteEntities;
        String site = query.site();
        if (site == null) {
            siteEntities = siteRepository.findAll().toArray(SiteEntity[]::new);
        } else {
            siteEntities = new SiteEntity[]{
                    siteRepository.findByUrl(site).orElseThrow(NotIndexedSiteException::new)};
        }
        if (areNotIndexedSites(siteEntities)) {
            throw new NotIndexedSiteException();
        }
        return siteEntities;
    }

    private boolean areNotIndexedSites(SiteEntity[] siteEntities) {
        return siteEntities.length == 0 ||
                Arrays.stream(siteEntities)
                        .map(indexRepository::countIndexesBySite)
                        .allMatch(Predicate.isEqual(0));
    }

    private Map<PageEntity, Float> getSearchResults(Query query, String[] queryLemmas) throws RuntimeException {
        SiteEntity[] siteEntities = getSites(query);
        Map<PageEntity, List<LemmaEntity>> pages = Arrays.stream(siteEntities)
                .map(i -> getRelatedPagesInfo(i, queryLemmas))
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (pages.isEmpty()) {
            return Collections.emptyMap();
        }
        return getPagesRelevance(pages);
    }

    private Map<PageEntity, List<LemmaEntity>> getRelatedPagesInfo(SiteEntity siteEntity, String[] queryLemmas) {
        List<LemmaEntity> lemmaEntities = lemmaRepository.findAllByLemmasSite(siteEntity, queryLemmas);
        if (lemmaEntities.size() != queryLemmas.length) {
            return Collections.emptyMap();
        }
        filterInfrequentLemmas(lemmaEntities, siteEntity);
        List<PageEntity> relatedPages = getRelatedPages(lemmaEntities);
        if (relatedPages.size() == 0) {
            return Collections.emptyMap();
        }
        return relatedPages.stream()
                .collect(HashMap::new,
                        (map, page) -> map.put(page, lemmaEntities),
                        HashMap::putAll);
    }

    private void filterInfrequentLemmas(List<LemmaEntity> lemmaEntities, SiteEntity siteEntity) {
        int pagesCount = siteEntity.getPages().size();
        lemmaEntities.sort(
                Comparator.comparing(LemmaEntity::getFrequency, Comparator.reverseOrder()));
        Iterator<LemmaEntity> iteratorLemmas = lemmaEntities.iterator();
        while (iteratorLemmas.hasNext() && lemmaEntities.size() > 1) {
            if (iteratorLemmas.next().getFrequency() > (pagesCount * INFREQUENCY_FACTOR)) {
                iteratorLemmas.remove();
            }
        }
    }

    private List<PageEntity> getRelatedPages(List<LemmaEntity> lemmaEntities) {
        List<PageEntity> pages = new ArrayList<>();
        lemmaEntities.sort(Comparator.comparing(LemmaEntity::getFrequency));
        boolean firstItem = true;
        for (LemmaEntity lemma : lemmaEntities) {
            if (firstItem) {
                pages = indexRepository.findPagesByLemma(lemma);
                firstItem = false;
            } else {
                pages = indexRepository.findPagesByLemmaInPages(lemma, pages);
            }
        }
        return pages;
    }

    private Map<PageEntity, Float> getPagesRelevance(Map<PageEntity, List<LemmaEntity>> pages) {
        Map<PageEntity, Float> pagesRelevance = pages.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, this::getAbsoluteRelevance));
        float maxAbsoluteRelevance = Collections.max(pagesRelevance.values(), Float::compareTo);
        return pagesRelevance.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        i -> i.getValue() / maxAbsoluteRelevance));

    }

    private float getAbsoluteRelevance(Map.Entry<PageEntity, List<LemmaEntity>> entry) {
        return indexRepository.findAllByPageLemmaIn(entry.getKey(), entry.getValue()).stream()
                .map(IndexEntity::getRank)
                .reduce(0F, Float::sum);
    }

    private SearchSuccessfulResponse generateResponse(Map<PageEntity, Float> pages,
                                                      Query query,
                                                      String[] queryLemmas) {
        List<SearchResult> results = pages.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .skip(query.offset())
                .limit(query.limit())
                .map(i -> generateSearchResult(queryLemmas, i))
                .toList();
        return new SearchSuccessfulResponse(true, pages.size(), results);
    }

    private SearchResult generateSearchResult(String[] queryLemmas, Map.Entry<PageEntity, Float> pageEntry) {
        PageEntity page = pageEntry.getKey();
        SiteEntity site = page.getSite();
        float relevance = pageEntry.getValue();
        return new SearchResult(site.getUrl(),
                site.getName(),
                page.getPath(),
                pageParsingUtils.getTitle(page.getContent()),
                getSnippet(page, queryLemmas),
                relevance);
    }

    private String getSnippet(PageEntity pageEntity, String[] queryLemmas) {
        return snippetService.getSnippet(pageEntity.getContent(), queryLemmas);
    }
}
