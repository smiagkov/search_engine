package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import searchengine.config.PagesCollectorConfig;
import searchengine.exceptions.DuplicatePageException;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;
import searchengine.utils.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Setter
@Getter
public class PagesCollector extends RecursiveTask<PagesCollectionEndType> {
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static LemmaRepository lemmaRepository;
    private static IndexRepository indexRepository;
    private static LemmaUtils lemmaUtils;
    private static PagesCollectorConfig params;
    private static PageParsingUtils pageParsingUtils;
    private static TextUtils textUtils;
    private String path;
    private final SiteEntity site;
    private static boolean isStartedIndexing;
    private final static Object lock = new Object();

    public PagesCollector(String path, SiteEntity site) {
        this.path = path;
        this.site = site;
    }

    public PagesCollector(SiteEntity rootPage) {
        this(pageParsingUtils.getRelativePath(rootPage.getUrl()), rootPage);
    }

    @Override
    protected PagesCollectionEndType compute() throws RuntimeException {
        Document document;
        Connection.Response response;
        String[] children = {};
        PageEntity page;
        if (!isStartedIndexing) {
            return PagesCollectionEndType.INTERRUPTED;
        }
        synchronized (lock) {
            page = new PageEntity(site, path);
            try {
                pageRepository.saveIfNotExists(page);
            } catch (DuplicatePageException e) {
                return PagesCollectionEndType.COMPLETED;
            }
        }
        try {
            response = pageParsingUtils.getPageResponse(path, new URL(site.getUrl()));
            document = response.parse();
            page.update(response.statusCode(), document.html());
            if (response.statusCode() >= 400) {
                return PagesCollectionEndType.COMPLETED;
            }
            addLemmasAndIndexesToDB(site, page);
            children = getChildPages(document);
        } catch (HttpStatusException e) {
            page.update(e.getStatusCode());
        } catch (IOException e) {
            page.update(HttpStatus.INTERNAL_SERVER_ERROR.value());
        } catch (Exception e) {
            page.update(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.toString());
        } finally {
            pageRepository.update(page, page.getCode(), page.getContent());
            site.update();
            siteRepository.updateStatusTime(site, site.getStatusTime());
        }
        return ForkJoinTask.invokeAll(new ArrayList<>(getSubTasks(children))).stream()
                .map(ForkJoinTask::join)
                .anyMatch(i -> i.equals(PagesCollectionEndType.INTERRUPTED)) ?
                PagesCollectionEndType.INTERRUPTED :
                PagesCollectionEndType.COMPLETED;
    }

    private String[] getChildPages(Document document) {
        String[] filteredPages;
        Set<String> parsedPages = document.select("a[href]").stream()
                .map(e -> e.attr("href"))
                .filter(s -> pageParsingUtils.isValidChildLink(s, site.getUrl()))
                .map(s -> s.startsWith("/") ? s : pageParsingUtils.getRelativePath(s))
                .collect(Collectors.toSet());
        Set<String> savedPages = pageRepository.findAllBySitePathIn(parsedPages, site);
        filteredPages = parsedPages.stream()
                .filter(Predicate.not(savedPages::contains))
                .toArray(String[]::new);
        return filteredPages;
    }

    private List<PagesCollector> getSubTasks(String[] pages) {
        return Arrays.stream(pages)
                .map(e -> new PagesCollector(e, site))
                .toList();
    }

    public static void addRepositories(SiteRepository siteRepository, PageRepository pageRepository,
                                       LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        PagesCollector.pageRepository = pageRepository;
        PagesCollector.siteRepository = siteRepository;
        PagesCollector.lemmaRepository = lemmaRepository;
        PagesCollector.indexRepository = indexRepository;
    }

    public static void setParams(PagesCollectorConfig params) {
        PagesCollector.params = params;
    }

    public static void setUtils(PageParsingUtils pageParsingUtils, TextUtils textUtils,
                                LemmaUtils lemmaUtils) {
        PagesCollector.pageParsingUtils = pageParsingUtils;
        PagesCollector.textUtils = textUtils;
        PagesCollector.lemmaUtils = lemmaUtils;
    }

    public static void setLemmaUtils(LemmaUtils lemmaUtils) {
        PagesCollector.lemmaUtils = lemmaUtils;
    }

    public static void setStartedParsing(boolean isStartedIndexing) {
        PagesCollector.isStartedIndexing = isStartedIndexing;
    }

    public static void stopParsing() {
        isStartedIndexing = false;
    }

    public void addLemmasAndIndexesToDB(SiteEntity site, PageEntity page) {
        Map<String, Integer> lemmas = lemmaUtils.getLemmasStatistics(
                textUtils.removeHtmlTags(page.getContent()));
        for (Map.Entry<String, Integer> lemmaEntry : lemmas.entrySet()) {
            LemmaEntity lemma = addOrUpdateLemma(site, lemmaEntry);
            indexRepository.save(new IndexEntity(page, lemma, lemmaEntry.getValue()));
        }
    }

    private LemmaEntity addOrUpdateLemma(SiteEntity site, Map.Entry<String, Integer> lemmaEntry) {
        LemmaEntity lemma;
        synchronized (lock) {
            Optional<LemmaEntity> optionalLemma = lemmaRepository.findBySiteAndLemma(site, lemmaEntry.getKey());
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
}
